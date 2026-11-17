package dev.portfolio.web3.tx.infrastructure;

import dev.portfolio.web3.tx.domain.TransactionRecord;
import dev.portfolio.web3.tx.domain.TransactionStatus;
import dev.portfolio.web3.tx.domain.TransactionStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTransactionStore implements TransactionStore {
    private static final RowMapper<TransactionRecord> ROW_MAPPER = (rs, rowNum) -> new TransactionRecord(
            rs.getString("idempotency_key"), rs.getLong("chain_id"), rs.getString("tx_hash"),
            TransactionStatus.valueOf(rs.getString("status")), Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at")), (Long) rs.getObject("block_number"),
            rs.getLong("confirmations"), rs.getString("failure_reason"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcTransactionStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<TransactionRecord> findByKey(String idempotencyKey) {
        List<TransactionRecord> records = jdbcTemplate.query(
                "SELECT * FROM transaction_record WHERE idempotency_key = ?", ROW_MAPPER, idempotencyKey);
        return records.stream().findFirst();
    }

    @Override
    public Optional<TransactionRecord> findByHash(String transactionHash) {
        List<TransactionRecord> records = jdbcTemplate.query("""
                SELECT * FROM transaction_record WHERE tx_hash = ?
                ORDER BY created_at DESC
                """, ROW_MAPPER, transactionHash);
        return records.stream().findFirst();
    }

    @Override
    public boolean createPending(TransactionRecord record) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO transaction_record (idempotency_key, chain_id, tx_hash, status, created_at,
                      updated_at, block_number, confirmations, failure_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, record.idempotencyKey(), record.chainId(), record.txHash(), record.status().name(),
                    record.createdAt().toString(), record.updatedAt().toString(), record.blockNumber(),
                    record.confirmations(), record.failureReason());
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    @Override
    public void update(TransactionRecord record) {
        jdbcTemplate.update("""
                UPDATE transaction_record SET status = ?, updated_at = ?, block_number = ?,
                  confirmations = ?, failure_reason = ?
                WHERE idempotency_key = ?
                """, record.status().name(), record.updatedAt().toString(), record.blockNumber(),
                record.confirmations(), record.failureReason(), record.idempotencyKey());
    }
}
