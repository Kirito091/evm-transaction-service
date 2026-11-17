package dev.portfolio.web3.tx.infrastructure;

import dev.portfolio.web3.tx.application.TransactionService;
import dev.portfolio.web3.tx.domain.ChainReceipt;
import dev.portfolio.web3.tx.domain.ReceiptState;
import dev.portfolio.web3.tx.domain.TransactionGateway;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DemoTransactionGateway implements TransactionGateway {
    private final ConcurrentHashMap<String, AtomicLong> refreshCount = new ConcurrentHashMap<>();

    @Override
    public String broadcast(String signedRawTransaction) {
        String hash = TransactionService.hashRawTransaction(signedRawTransaction);
        refreshCount.putIfAbsent(hash, new AtomicLong());
        return hash;
    }

    @Override
    public Optional<ChainReceipt> receipt(String transactionHash) {
        AtomicLong count = refreshCount.get(transactionHash);
        if (count == null) {
            return Optional.empty();
        }
        return Optional.of(new ChainReceipt(ReceiptState.SUCCESS, 100, count.incrementAndGet(), null));
    }
}
