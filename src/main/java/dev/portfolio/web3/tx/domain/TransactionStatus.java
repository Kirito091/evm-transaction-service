package dev.portfolio.web3.tx.domain;

public enum TransactionStatus {
    PENDING_BROADCAST,
    SUBMITTED,
    CONFIRMED,
    REVERTED;

    public boolean terminal() {
        return this == CONFIRMED || this == REVERTED;
    }
}
