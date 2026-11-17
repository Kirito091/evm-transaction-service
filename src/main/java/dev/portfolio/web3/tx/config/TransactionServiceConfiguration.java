package dev.portfolio.web3.tx.config;

import dev.portfolio.web3.tx.application.TransactionService;
import dev.portfolio.web3.tx.domain.TransactionGateway;
import dev.portfolio.web3.tx.domain.TransactionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionServiceConfiguration {
    @Bean
    TransactionService transactionService(TransactionStore store, TransactionGateway gateway,
                                          @Value("${chain.id:11155111}") long chainId,
                                          @Value("${chain.confirmations:3}") long confirmations) {
        return new TransactionService(store, gateway, chainId, confirmations);
    }
}
