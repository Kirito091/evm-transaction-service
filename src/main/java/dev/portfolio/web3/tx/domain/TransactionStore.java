package dev.portfolio.web3.tx.domain;

import java.util.Optional;

public interface TransactionStore {
    Optional<TransactionRecord> findByKey(String idempotencyKey);

    Optional<TransactionRecord> findByHash(String transactionHash);

    boolean createPending(TransactionRecord record);

    void update(TransactionRecord record);
}
