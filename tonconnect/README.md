# TonConnect module

## Maven [![Maven Central][maven-central-svg]][maven-central]

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>tonconnect</artifactId>
    <version>2.1.1</version>
</dependency>
```

## Jitpack

```xml

<dependency>
    <groupId>org.ton.ton4j</groupId>
    <artifactId>tonconnect</artifactId>
    <version>2.1.1</version>
</dependency>
```

## Description

Please follow the [official documentation](https://docs.ton.org/develop/dapps/ton-connect/sign#how-does-it-work) for
more details.

## Usage

Use `TonConnect.verifyProof` for authentication. It checks the expected domain, chain,
challenge and timestamp window, verifies the signature, then calls your challenge store
to consume the challenge atomically. A successful proof can authenticate only once.

Generate an unpredictable challenge on the backend and retain it in the user's login
session. Send its value as the TonConnect `ton_proof` payload; the wallet supplies the
signed proof. The expected domain, chain and challenge must come from backend state,
not from the submitted proof. Domains are case-insensitive ASCII DNS names, including
punycode; URLs, paths, ports, whitespace, Unicode names and trailing dots are rejected.

The example below binds each challenge to an expected wallet address, domain, chain and
expiry. `trustedAccount` must contain a public key the backend has verified belongs to
that address on the expected chain (for example, by querying its wallet contract).
A public key supplied by the client alone is insufficient to establish this binding.

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

    boolean authenticate(TonProof proof, WalletAccount trustedAccount, String sessionChallenge)
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
        return TonConnect.verifyProof(proof, trustedAccount, context, store);
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
across all login sessions. Signature and context failures do not consume a challenge.

The map is a single-process example. For multiple backend instances, use shared storage
with the same atomic check-and-delete behavior, and remove expired entries periodically.

`verifyProofSignature` checks only the cryptographic signature. The deprecated
`checkProof(proof, account)` remains a compatible alias for that method; neither method
alone authenticates a user or prevents replay.

## Tests

Local-network integration tests are disabled by default. Enable them with
`-Dtonconnect.localIntegrationTests=true` only when a local TON node is available.

[maven-central-svg]: https://img.shields.io/maven-central/v/org.ton.ton4j/tonconnect

[maven-central]: https://mvnrepository.com/artifact/org.ton.ton4j/tonconnect

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org
