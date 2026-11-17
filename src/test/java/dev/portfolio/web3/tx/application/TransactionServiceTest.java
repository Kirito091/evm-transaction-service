package dev.portfolio.web3.tx.application;

import dev.portfolio.web3.tx.domain.ChainReceipt;
import dev.portfolio.web3.tx.domain.ReceiptState;
import dev.portfolio.web3.tx.domain.TransactionGateway;
import dev.portfolio.web3.tx.domain.TransactionRecord;
import dev.portfolio.web3.tx.domain.TransactionStatus;
import dev.portfolio.web3.tx.domain.TransactionStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionServiceTest {
    private static final long CHAIN_ID = 11_155_111L;
    private static final String RAW = "0x02c0";

    @Test
    void sameIdempotencyKeyAndPayloadReturnTheSameSubmittedTransaction() {
        FakeStore store = new FakeStore();
        FakeGateway gateway = new FakeGateway();
        TransactionService service = new TransactionService(store, gateway, CHAIN_ID, 3);

        TransactionRecord first = service.submit("client-request-1", CHAIN_ID, RAW);
        TransactionRecord replay = service.submit("client-request-1", CHAIN_ID, RAW);

        assertThat(first.txHash()).isEqualTo(replay.txHash());
        assertThat(replay.status()).isEqualTo(TransactionStatus.SUBMITTED);
        assertThat(gateway.broadcastCalls).isEqualTo(1);
    }

    @Test
    void oneIdempotencyKeyCannotBeReusedForDifferentSignedBytes() {
        TransactionService service = new TransactionService(new FakeStore(), new FakeGateway(), CHAIN_ID, 3);
        service.submit("client-request-2", CHAIN_ID, RAW);

        assertThatThrownBy(() -> service.submit("client-request-2", CHAIN_ID, "0x03c0"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void receiptIsNotFinalUntilTheRequiredConfirmationDepthIsReached() {
        FakeStore store = new FakeStore();
        FakeGateway gateway = new FakeGateway();
        TransactionService service = new TransactionService(store, gateway, CHAIN_ID, 3);
        TransactionRecord submitted = service.submit("client-request-3", CHAIN_ID, RAW);
        gateway.receipt = new ChainReceipt(ReceiptState.SUCCESS, 100, 2, null);

        TransactionRecord stillPending = service.refresh(submitted.txHash());

        assertThat(stillPending.status()).isEqualTo(TransactionStatus.SUBMITTED);
        assertThat(stillPending.confirmations()).isEqualTo(2);

        gateway.receipt = new ChainReceipt(ReceiptState.SUCCESS, 100, 3, null);
        TransactionRecord confirmed = service.refresh(submitted.txHash());

        assertThat(confirmed.status()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(confirmed.blockNumber()).isEqualTo(100L);
    }

    private static final class FakeGateway implements TransactionGateway {
        private int broadcastCalls;
        private ChainReceipt receipt;

        @Override
        public String broadcast(String signedRawTransaction) {
            broadcastCalls++;
            return TransactionService.hashRawTransaction(signedRawTransaction);
        }

        @Override
        public Optional<ChainReceipt> receipt(String transactionHash) {
            return Optional.ofNullable(receipt);
        }
    }

    private static final class FakeStore implements TransactionStore {
        private final Map<String, TransactionRecord> byKey = new HashMap<>();
        private final Map<String, String> keysByHash = new HashMap<>();

        @Override
        public Optional<TransactionRecord> findByKey(String idempotencyKey) {
            return Optional.ofNullable(byKey.get(idempotencyKey));
        }

        @Override
        public Optional<TransactionRecord> findByHash(String transactionHash) {
            String key = keysByHash.get(transactionHash);
            return Optional.ofNullable(key == null ? null : byKey.get(key));
        }

        @Override
        public boolean createPending(TransactionRecord record) {
            if (byKey.containsKey(record.idempotencyKey())) {
                return false;
            }
            byKey.put(record.idempotencyKey(), record);
            keysByHash.put(record.txHash(), record.idempotencyKey());
            return true;
        }

        @Override
        public void update(TransactionRecord record) {
            byKey.put(record.idempotencyKey(), record);
        }
    }
}
