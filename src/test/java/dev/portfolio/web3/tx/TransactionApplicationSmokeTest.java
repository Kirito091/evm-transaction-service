package dev.portfolio.web3.tx;

import dev.portfolio.web3.tx.application.TransactionService;
import dev.portfolio.web3.tx.domain.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransactionApplicationSmokeTest {
    @Autowired
    private TransactionService transactionService;

    @Test
    void demoApplicationPersistsAndConfirmsAReplaySafeTransaction() {
        var first = transactionService.submit("smoke-test", 11_155_111L, "0x02c0");
        var replay = transactionService.submit("smoke-test", 11_155_111L, "0x02c0");
        assertThat(replay.txHash()).isEqualTo(first.txHash());
        assertThat(replay.status()).isEqualTo(TransactionStatus.SUBMITTED);

        transactionService.refresh(first.txHash());
        transactionService.refresh(first.txHash());
        var confirmed = transactionService.refresh(first.txHash());

        assertThat(confirmed.status()).isEqualTo(TransactionStatus.CONFIRMED);
    }
}
