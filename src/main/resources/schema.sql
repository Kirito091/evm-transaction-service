CREATE TABLE IF NOT EXISTS transaction_record (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    chain_id BIGINT NOT NULL,
    tx_hash VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at VARCHAR(40) NOT NULL,
    updated_at VARCHAR(40) NOT NULL,
    block_number BIGINT,
    confirmations BIGINT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_transaction_hash ON transaction_record (tx_hash);
