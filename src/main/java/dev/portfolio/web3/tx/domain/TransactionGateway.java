package dev.portfolio.web3.tx.domain;

import java.util.Optional;

public interface TransactionGateway {
    String broadcast(String signedRawTransaction);

    Optional<ChainReceipt> receipt(String transactionHash);
}
