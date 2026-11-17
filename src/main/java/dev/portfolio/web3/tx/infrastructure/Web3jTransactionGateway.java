package dev.portfolio.web3.tx.infrastructure;

import dev.portfolio.web3.tx.domain.ChainReceipt;
import dev.portfolio.web3.tx.domain.ReceiptState;
import dev.portfolio.web3.tx.domain.TransactionGateway;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import java.util.Optional;

public final class Web3jTransactionGateway implements TransactionGateway {
    private final Web3j web3j;
    private final long expectedChainId;

    public Web3jTransactionGateway(String rpcUrl, long expectedChainId) {
        if (rpcUrl == null || rpcUrl.isBlank()) {
            throw new IllegalArgumentException("chain.rpc-url is required when chain.mode=rpc");
        }
        if (expectedChainId < 1) {
            throw new IllegalArgumentException("chain.id must be positive");
        }
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.expectedChainId = expectedChainId;
    }

    @Override
    public String broadcast(String signedRawTransaction) {
        try {
            verifyConfiguredChain();
            EthSendTransaction response = web3j.ethSendRawTransaction(signedRawTransaction).send();
            if (response.hasError()) {
                throw new IllegalStateException("RPC rejected the signed transaction: " + response.getError().getMessage());
            }
            return response.getTransactionHash();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to submit signed transaction to configured RPC", exception);
        }
    }

    @Override
    public Optional<ChainReceipt> receipt(String transactionHash) {
        try {
            verifyConfiguredChain();
            EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(transactionHash).send();
            if (response.hasError()) {
                throw new IllegalStateException("RPC error while reading receipt: " + response.getError().getMessage());
            }
            Optional<TransactionReceipt> optionalReceipt = response.getTransactionReceipt();
            if (optionalReceipt.isEmpty()) {
                return Optional.empty();
            }
            TransactionReceipt receipt = optionalReceipt.get();
            EthBlockNumber headResponse = web3j.ethBlockNumber().send();
            if (headResponse.hasError()) {
                throw new IllegalStateException("RPC error while reading confirmation height: "
                        + headResponse.getError().getMessage());
            }
            long block = receipt.getBlockNumber().longValueExact();
            long head = headResponse.getBlockNumber().longValueExact();
            long confirmations = Math.max(0, head - block + 1);
            boolean success = "0x1".equalsIgnoreCase(receipt.getStatus());
            return Optional.of(new ChainReceipt(success ? ReceiptState.SUCCESS : ReceiptState.REVERTED,
                    block, confirmations, success ? null : "transaction receipt status is 0x0"));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read transaction receipt from configured RPC", exception);
        }
    }

    private void verifyConfiguredChain() throws Exception {
        EthChainId response = web3j.ethChainId().send();
        if (response.hasError()) {
            throw new IllegalStateException("RPC error while reading chain id: " + response.getError().getMessage());
        }
        long actualChainId = response.getChainId().longValueExact();
        if (actualChainId != expectedChainId) {
            throw new IllegalStateException("configured chain id " + expectedChainId
                    + " does not match RPC chain id " + actualChainId);
        }
    }
}
