package dev.portfolio.web3.tx.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitTransactionRequest(
        @Min(1) long chainId,
        @NotBlank
        @Pattern(regexp = "^(0x|0X)?[0-9a-fA-F]+$", message = "signedRawTransaction must be hexadecimal bytes")
        @Size(max = 120000)
        String signedRawTransaction) {
}
