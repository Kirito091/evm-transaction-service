# EVM Transaction Service

A Java/Spring Boot service for submitting an **already-signed** EVM transaction and tracking its receipt. It focuses on two practical backend concerns: idempotent submission and confirmation-aware transaction state.

## What it demonstrates

- A caller-signed raw transaction is relayed without the service ever handling a private key.
- An `Idempotency-Key` is bound to one chain and transaction hash; retries return the existing transaction.
- Transaction state progresses from `PENDING_BROADCAST` to `SUBMITTED`, then to `CONFIRMED` or `REVERTED` only after the configured confirmation depth.
- JDBC persistence stores the hash and state, not the raw signed transaction.
- A deterministic demo gateway is the default; an optional web3j RPC gateway can be enabled explicitly.

## Run locally

Requirements: Java 21 and Maven 3.6.3+.

```bash
mvn test
mvn spring-boot:run
```

The service listens on port `8081` by default. Its local H2 database is file-backed under `data/` so transaction state survives a restart; that generated directory is ignored by Git. Tests use a separate in-memory database. Demo submission example:

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-transfer-001" \
  -d '{"chainId":11155111,"signedRawTransaction":"0x02c0"}'
```

Refresh the returned hash three times to see the deterministic demo receipt reach the configured confirmation depth:

```bash
curl -X POST http://localhost:8081/api/v1/transactions/0x.../refresh
```

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/transactions` | Idempotent broadcast of caller-signed bytes |
| `GET` | `/api/v1/transactions/{hash}` | Read saved transaction state |
| `POST` | `/api/v1/transactions/{hash}/refresh` | Check receipt and confirmation depth |

## Safety boundary

The demo uses an in-memory chain adapter and never broadcasts anything. RPC mode requires `CHAIN_MODE=rpc`, `CHAIN_ID`, and `RPC_URL`; the service checks `eth_chainId` before broadcasting or refreshing and fails closed on a mismatch. Use a test network only. Never submit a private key or seed phrase. Raw signed transactions are broadcast-capable bearer artifacts, so do not paste them into public services or issue trackers.

The concise behavioral specification is in [`docs/SPEC.md`](docs/SPEC.md).

