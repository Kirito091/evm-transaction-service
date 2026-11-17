package dev.portfolio.web3.tx.api;

import dev.portfolio.web3.tx.application.TransactionService;
import dev.portfolio.web3.tx.domain.TransactionRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public TransactionRecord submit(@RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
                                    @Valid @RequestBody SubmitTransactionRequest request) {
        return transactionService.submit(idempotencyKey, request.chainId(), request.signedRawTransaction());
    }

    @GetMapping("/{transactionHash}")
    public TransactionRecord get(@PathVariable String transactionHash) {
        return transactionService.find(transactionHash);
    }

    @PostMapping("/{transactionHash}/refresh")
    public TransactionRecord refresh(@PathVariable String transactionHash) {
        return transactionService.refresh(transactionHash);
    }
}
