# Codex Instructions — ton4j

## Scope
You are working on ton4j, a multi-module Java SDK for the TON blockchain.

## First action
Before making changes:
1. Read AGENTS.md (root)
2. Identify the target Maven module
3. Run tests only for that module unless instructed otherwise

## Hard constraints
- Do NOT rename or remove public APIs without explicit instruction.
- Do NOT change serialization, hashing, or encoding logic without adding tests.
- Do NOT add new dependencies unless requested.
- Assume backward compatibility is required.

## Native-related rules
Some modules depend on external binaries (tonlibjson, emulator, lite-client):
- Do not assume native libs are available.
- Gate native tests behind Maven profiles or JUnit assumptions.
- Prefer mock/stub-based tests when possible.

## Preferred commands
- Build single module:
  mvn -pl <module> -am test
- Skip native tests:
  mvn -DskipNativeTests test

## Output expectations
- Small, focused diffs
- Explain why a change is safe
- Point out any API or behavior change explicitly
