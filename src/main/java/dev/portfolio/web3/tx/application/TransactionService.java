package dev.portfolio.web3.tx.application;

import dev.portfolio.web3.tx.domain.ChainReceipt;
import dev.portfolio.web3.tx.domain.ReceiptState;
import dev.portfolio.web3.tx.domain.TransactionGateway;
import dev.portfolio.web3.tx.domain.TransactionRecord;
import dev.portfolio.web3.tx.domain.TransactionStatus;
import dev.portfolio.web3.tx.domain.TransactionStore;
import org.web3j.crypto.Hash;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class TransactionService {
    private final TransactionStore store;
    private final TransactionGateway gateway;
    private final long expectedChainId;
    private final long confirmationThreshold;

    public TransactionService(TransactionStore store, TransactionGateway gateway,
                              long expectedChainId, long confirmationThreshold) {
        this.store = Objects.requireNonNull(store, "store");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        if (expectedChainId < 1 || confirmationThreshold < 1) {
            throw new IllegalArgumentException("chain id and confirmation threshold must be positive");
        }
        this.expectedChainId = expectedChainId;
        this.confirmationThreshold = confirmationThreshold;
    }

    public TransactionRecord submit(String idempotencyKey, long chainId, String signedRawTransaction) {
        String key = validateIdempotencyKey(idempotencyKey);
        if (chainId != expectedChainId) {
            throw new IllegalArgumentException("request chain id does not match configured chain id");
        }
        String raw = normalizeRawTransaction(signedRawTransaction);
        String expectedHash = hashRawTransaction(raw);
        TransactionRecord existing = store.findByKey(key).orElse(null);
        if (existing != null) {
            if (existing.chainId() != chainId || !existing.txHash().equalsIgnoreCase(expectedHash)) {
                throw new IdempotencyConflictException("idempotency key is already bound to another transaction");
            }
            if (existing.status() != TransactionStatus.PENDING_BROADCAST) {
                return existing;
            }
            return broadcastAndSave(existing, raw, expectedHash);
        }

        Instant now = Instant.now();
        TransactionRecord pending = new TransactionRecord(key, chainId, expectedHash,
                TransactionStatus.PENDING_BROADCAST, now, now, null, 0, null);
        if (!store.createPending(pending)) {
            TransactionRecord raced = store.findByKey(key).orElse(null);
            if (raced == null) {
                throw new IdempotencyConflictException("transaction hash is already tracked under another key");
            }
            if (raced.chainId() != chainId || !raced.txHash().equalsIgnoreCase(expectedHash)) {
                throw new IdempotencyConflictException("idempotency key is already bound to another transaction");
            }
            if (raced.status() != TransactionStatus.PENDING_BROADCAST) {
                return raced;
            }
            return broadcastAndSave(raced, raw, expectedHash);
        }
        return broadcastAndSave(pending, raw, expectedHash);
    }

    public TransactionRecord refresh(String transactionHash) {
        String normalizedHash = normalizeHash(transactionHash);
        TransactionRecord record = store.findByHash(normalizedHash)
                .orElseThrow(() -> new TransactionNotFoundException(normalizedHash));
        if (record.status().terminal()) {
            return record;
        }
        ChainReceipt receipt = gateway.receipt(normalizedHash).orElse(null);
        if (receipt == null) {
            return record;
        }
        TransactionStatus status = record.status();
        if (receipt.confirmations() >= confirmationThreshold) {
            status = receipt.state() == ReceiptState.SUCCESS ? TransactionStatus.CONFIRMED : TransactionStatus.REVERTED;
        }
        TransactionRecord updated = record.withReceipt(receipt, status, Instant.now());
        store.update(updated);
        return updated;
    }

    public TransactionRecord find(String transactionHash) {
        String normalizedHash = normalizeHash(transactionHash);
        return store.findByHash(normalizedHash).orElseThrow(() -> new TransactionNotFoundException(normalizedHash));
    }

    private TransactionRecord broadcastAndSave(TransactionRecord pending, String raw, String expectedHash) {
        String returnedHash = normalizeHash(gateway.broadcast(raw));
        if (!expectedHash.equalsIgnoreCase(returnedHash)) {
            throw new IllegalStateException("RPC returned a transaction hash that differs from the signed payload");
        }
        TransactionRecord submitted = pending.withBroadcast(returnedHash, Instant.now());
        store.update(submitted);
        return submitted;
    }

    private static String validateIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must contain 1 to 128 characters");
        }
        return value.trim();
    }

    private static String normalizeRawTransaction(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("signed transaction is required");
        }
        String raw = value.trim();
        String payload = raw.startsWith("0x") || raw.startsWith("0X") ? raw.substring(2) : raw;
        if (payload.length() < 2 || payload.length() % 2 != 0 || !payload.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("signed transaction must be even-length hexadecimal bytes");
        }
        return "0x" + payload.toLowerCase(Locale.ROOT);
    }

    private static String normalizeHash(String value) {
        if (value == null || !value.matches("(?i)^0x[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("transaction hash must be a 32-byte 0x-prefixed hash");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public static String hashRawTransaction(String signedRawTransaction) {
        return Hash.sha3(normalizeRawTransaction(signedRawTransaction));
    }
}
