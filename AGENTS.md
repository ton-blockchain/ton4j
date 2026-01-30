# AGENTS.md — ton4j (TON Java SDK)

## What this repo is
ton4j is a multi-module Maven project providing Java libraries for The Open Network (TON).
Modules are independent; prefer the smallest module that satisfies a change.

## Fast start (local)
- Requirements: Java 11+ (library baseline), Maven
- Build everything: `mvn -q -DskipTests package`
- Run tests: `mvn -q test`
- Build only one module (example): `mvn -q -pl cell -am test`

## How to choose the right module
- If the change is about addresses: `address`
- BoC/Cell serialization, TL-B: `cell`, `bitstring`, `tlb`
- Tonlib native wrapper: `tonlib` (requires native tonlibjson)
- Lite-client native wrapper: `liteclient` (requires native lite-client)
- Smart contract abstractions/wallets: `smartcontract`
- Lite-client over ADNL: `adnl`
- REST clients: `toncenter`, `toncenter-indexer-v3`
- TonConnect protocol: `tonconnect`
- Mnemonic: `mnemonic`
- Various helpful methods across all modules: `utils`
- Smart contract compilation: `tolk`, `func` and `fift`
- Smart contract disassembler: `disassembler`
- TON TVM and transaction emulator: `emulator`
- TON RocksDB direct access, data export: `exporter`
(If unsure: search usages first and follow existing patterns.)

## Non-negotiables (style & safety)
- Keep public APIs backward compatible unless the PR explicitly says "breaking change".
- Prefer adding tests in the same module.
- Do not add new dependencies unless necessary; justify them in PR description.
- No secrets, no private keys, no mainnet funds in tests/examples.
- Keep logging quiet in unit tests.

## When working on native/binary wrappers
Some modules wrap external binaries/shared libraries. If a test requires them:
- Look at `utils` module `Utils` class if there are methods for fetching them.
- Gate with an explicit Maven profile or JUnit assumption.
- Provide a “skip native” path so CI passes without local native libs.
- Document required env vars / paths in the module README.

## PR expectations
- Small, focused diffs.
- Update README/examples if user-facing behavior changes.
- Add/adjust tests.
