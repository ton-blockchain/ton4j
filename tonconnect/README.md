# TonConnect module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>tonconnect</artifactId>
    <version>2.2.0</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>tonconnect</artifactId>
    <version>2.2.0</version>
</dependency>
```

## Description

Please follow
the [official TonConnect proof specification](https://github.com/ton-blockchain/ton-connect/blob/main/spec/connect.md#address-proof-signature-ton_proof)
for
more details.

## Usage

Use `TonConnect.verifyProof` for authentication. It checks the expected domain, chain,
challenge and timestamp window, binds the wallet key to the claimed address using
`walletStateInit`, verifies the signature, then calls your challenge store to consume
the challenge atomically. A successful proof can authenticate only once.

Pass the wallet's untrusted `WalletAccount` response, including its base64 BoC
`walletStateInit`. The verifier requires a recognized wallet code and valid data layout,
derives the raw address from the StateInit hash and claimed workchain, and uses the
extracted public key only after the derived address matches `account.address`. An optional
`account.publicKey` must match the extracted key; it never chooses the verification key.
The local path supports V1R1–V1R3, V2R1–V2R2, V3R1–V3R2, V4R1–V4R2 and V5R1.
It accepts ordinary initial StateInit cells without split depth, tick-tock or libraries;
V4 plugins and V5 extensions must have empty initial dictionaries. Missing, malformed,
unsupported or mismatched StateInit data is rejected.

Generate an unpredictable challenge on the backend and retain it in the user's login
session. Send its value as the TonConnect `ton_proof` payload; the wallet supplies the
signed proof. The expected domain, chain and challenge must come from backend state,
not from the submitted proof. Domains are case-insensitive ASCII DNS names, including
punycode; URLs, paths, ports, whitespace, Unicode names and trailing dots are rejected.

The example below binds each challenge to an expected wallet address, domain, chain and
expiry when it is issued. Keep those original bindings in backend storage; do not create
or replace challenge entries from the submitted proof or account. `account` contains
the untrusted wallet response; `verifyProof` validates its key and address binding.

```java
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tonconnect.*;

final class TonProofAuthentication {
    private static final String DOMAIN = "login.example.com";
    private static final int CHAIN = -239;
    private static final long MAX_AGE_SECONDS = 300;
    private static final long FUTURE_SKEW_SECONDS = 30;
    private final Clock clock = Clock.systemUTC();
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, PendingChallenge> pending = new ConcurrentHashMap<>();

    String issueChallenge(String expectedWalletAddress) {
        String address = Address.of(expectedWalletAddress).toRaw();
        byte[] bytes = new byte[32];
        String challenge;
        PendingChallenge entry = new PendingChallenge(
                address, DOMAIN, CHAIN, clock.instant().getEpochSecond() + MAX_AGE_SECONDS);
        do {
            random.nextBytes(bytes);
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (pending.putIfAbsent(challenge, entry) != null);
        return challenge; // Save in the backend login session and send as ton_proof payload.
    }

    boolean authenticate(TonProof proof, WalletAccount account, String sessionChallenge)
            throws Exception {
        ProofVerificationContext context = new ProofVerificationContext(
                DOMAIN, CHAIN, sessionChallenge, clock.instant().getEpochSecond(),
                MAX_AGE_SECONDS, FUTURE_SKEW_SECONDS);

        ProofChallengeStore store = (challenge, address, domain, chain, now) -> {
            PendingChallenge entry = pending.get(challenge);
            return entry != null
                    && entry.address.equals(address)
                    && entry.domain.equals(domain)
                    && entry.chain == chain
                    && entry.expiresAt > now
                    && pending.remove(challenge, entry);
        };
        return TonConnect.verifyProof(proof, account, context, store);
    }

    private static final class PendingChallenge {
        final String address;
        final String domain;
        final int chain;
        final long expiresAt;

        PendingChallenge(String address, String domain, int chain, long expiresAt) {
            this.address = address;
            this.domain = domain;
            this.chain = chain;
            this.expiresAt = expiresAt;
        }
    }
}
```

Create the application session only when `authenticate` returns `true`, then clear the
login session's challenge. Fail closed if verification or challenge storage throws.
The store receives a canonical raw address and a normalized lower-case domain. It must
reject unknown, expired or mismatched entries and atomically remove the challenge once
across all login sessions. StateInit, key, signature and context failures do not consume
a challenge.

The map is a single-process example. For multiple backend instances, use shared storage
with the same atomic check-and-delete behavior, and remove expired entries periodically.

For other wallet code or StateInit layouts, use `verifyProofWithTrustedKey` with a
backend-owned resolver. It must obtain the 32-byte key from trusted, proof-verified
chain state for the exact canonical raw address and expected chain supplied to it.
An unauthenticated RPC response or the client's `publicKey` or `walletStateInit` is
insufficient. The same context and atomic challenge store are required; an optional
client public key must match the resolved key.

```java
import org.ton.ton4j.tonconnect.*;

final class TrustedTonProofAuthentication {
    interface VerifiedChainReader {
        // Read the key from authenticated contract state on this chain at this address.
        byte[] walletPublicKey(String canonicalRawAddress, int expectedChain) throws Exception;
    }

    static boolean authenticate(
            TonProof proof, WalletAccount account, ProofVerificationContext context,
            ProofChallengeStore store, VerifiedChainReader verifiedChain) throws Exception {
        TrustedWalletKeyResolver resolver =
                (address, chain) -> verifiedChain.walletPublicKey(address, chain);
        return TonConnect.verifyProofWithTrustedKey(proof, account, context, store, resolver);
    }
}
```

`verifyProofSignature` checks only the cryptographic signature.
`checkProof(proof, account)` remains its deprecated compatible alias. These helpers
trust client key material, do not bind the key to the wallet address, and do not
authenticate a user or prevent replay.

## Tests

Local-network integration tests are disabled by default. Enable them with
`-Dtonconnect.localIntegrationTests=true` only when a local TON node is available.

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/tonconnect

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/tonconnect

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org
