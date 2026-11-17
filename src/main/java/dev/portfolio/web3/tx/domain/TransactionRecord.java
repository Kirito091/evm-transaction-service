package dev.portfolio.web3.tx.domain;

import java.time.Instant;

public record TransactionRecord(
        String idempotencyKey,
        long chainId,
        String txHash,
        TransactionStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long blockNumber,
        long confirmations,
        String failureReason) {

    public TransactionRecord withBroadcast(String returnedHash, Instant now) {
        return new TransactionRecord(idempotencyKey, chainId, returnedHash, TransactionStatus.SUBMITTED,
                createdAt, now, blockNumber, confirmations, null);
    }

    public TransactionRecord withReceipt(ChainReceipt receipt, TransactionStatus updatedStatus, Instant now) {
        return new TransactionRecord(idempotencyKey, chainId, txHash, updatedStatus, createdAt, now,
                receipt.blockNumber(), receipt.confirmations(), receipt.failureReason());
    }
}
