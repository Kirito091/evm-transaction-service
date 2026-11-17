package dev.portfolio.web3.tx.domain;

public record ChainReceipt(ReceiptState state, long blockNumber, long confirmations, String failureReason) {
}
