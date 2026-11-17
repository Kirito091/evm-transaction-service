# EVM Transaction Service — Scope

## Goal

Demonstrate a Java/Spring Boot service that accepts an already-signed EVM transaction, broadcasts it, and tracks its receipt without ever handling a private key.

## User-visible behavior

- `POST /api/v1/transactions` accepts a chain id and signed raw transaction with an `Idempotency-Key` header.
- The service computes the transaction hash, records an idempotency entry, then broadcasts the raw signed transaction.
- Repeating the same key and payload returns the same transaction; reusing a key for a different payload returns a conflict.
- `GET /api/v1/transactions/{hash}` returns the saved state.
- `POST /api/v1/transactions/{hash}/refresh` checks a receipt and updates status only after the configured confirmation threshold.
- The default `demo` gateway is deterministic and does not connect to a wallet or network.
- The optional `rpc` gateway accepts a caller-signed transaction and only broadcasts/checks it; no private-key APIs exist.

## Invariants

- The service never receives, stores, logs, or derives a private key or seed phrase.
- The signed raw transaction is not persisted; only its chain id and hash are stored.
- An idempotency key is bound to one chain and one transaction hash.
- Terminal states (`CONFIRMED`, `REVERTED`) do not move backwards.
- RPC mode must be explicitly enabled and uses an externally supplied endpoint.

## Acceptance checks

- Invalid chain ids, malformed transaction bytes, and missing idempotency keys are rejected.
- Replaying the same request does not create a second logical transaction or broadcast after submission.
- Reusing a key for a different raw transaction is rejected as a conflict.
- A successful receipt below the confirmation threshold remains `SUBMITTED`.
- A reverted receipt is represented distinctly from an unconfirmed transaction.
- Maven tests pass on Java 21.
