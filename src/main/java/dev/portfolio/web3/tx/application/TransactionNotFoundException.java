package dev.portfolio.web3.tx.application;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String transactionHash) {
        super("Transaction not found: " + transactionHash);
    }
}
