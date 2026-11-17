package dev.portfolio.web3.tx.config;

import dev.portfolio.web3.tx.domain.TransactionGateway;
import dev.portfolio.web3.tx.infrastructure.DemoTransactionGateway;
import dev.portfolio.web3.tx.infrastructure.Web3jTransactionGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionGatewayConfiguration {
    @Bean
    @ConditionalOnProperty(name = "chain.mode", havingValue = "demo", matchIfMissing = true)
    TransactionGateway demoTransactionGateway() {
        return new DemoTransactionGateway();
    }

    @Bean
    @ConditionalOnProperty(name = "chain.mode", havingValue = "rpc")
    TransactionGateway rpcTransactionGateway(@Value("${chain.rpc-url:}") String rpcUrl,
                                             @Value("${chain.id}") long chainId) {
        return new Web3jTransactionGateway(rpcUrl, chainId);
    }
}
