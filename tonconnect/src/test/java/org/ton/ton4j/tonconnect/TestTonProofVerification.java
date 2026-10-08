package org.ton.ton4j.tonconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import com.iwebpp.crypto.TweetNaclFast;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.ton.ton4j.utils.Utils;

public class TestTonProofVerification {

  private static final String DOMAIN = "login.example";
  private static final String ADDRESS =
      "0:000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
  private static final int CHAIN = -239;
  private static final long NOW = 1_800_000_000L;
  private static final long MAX_AGE = 300;
  private static final long FUTURE_SKEW = 30;

  @Test
  public void acceptsFreshProofOnlyOnce() throws Exception {
    Fixture fixture = new Fixture();

    assertThat(fixture.verify()).isTrue();
    assertThat(fixture.verify()).isFalse();
    assertThat(fixture.store.challenges).doesNotContainKey(fixture.challenge);
  }

  @Test
  public void concurrentVerificationConsumesChallengeOnlyOnce() throws Exception {
    Fixture fixture = new Fixture();
    int attempts = 8;
    ExecutorService executor = Executors.newFixedThreadPool(attempts);
    CountDownLatch ready = new CountDownLatch(attempts);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int i = 0; i < attempts; i++) {
        results.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                  return fixture.verify();
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      int accepted = 0;
      for (Future<Boolean> result : results) {
        if (result.get(10, TimeUnit.SECONDS)) {
          accepted++;
        }
      }
      assertThat(accepted).isEqualTo(1);
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void rejectsProofSignedForAnotherDomainWithoutConsumingChallenge() throws Exception {
    Fixture fixture = new Fixture();
    fixture.proof.setDomain(domain("other.example"));
    fixture.sign();

    assertThat(fixture.verify()).isFalse();
    assertThat(fixture.store.calls).hasValue(0);
    fixture.proof.setDomain(domain(DOMAIN));
    fixture.sign();
    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void normalizesDomainCaseForVerifierAndStore() throws Exception {
    Fixture fixture = new Fixture();
    fixture.proof.setDomain(domain("LOGIN.EXAMPLE"));
    fixture.sign();
    ProofVerificationContext context =
        new ProofVerificationContext(
            "Login.Example", CHAIN, fixture.challenge, NOW, MAX_AGE, FUTURE_SKEW);

    assertThat(TonConnect.verifyProof(fixture.proof, fixture.account, context, fixture.store))
        .isTrue();
  }

  @Test
  public void consumesUsingCanonicalRawAddress() throws Exception {
    Fixture fixture = new Fixture();
    fixture.account.setAddress(ADDRESS.toUpperCase(Locale.ROOT));
    fixture.sign();

    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void rejectsAnotherChainWithoutConsumingChallenge() throws Exception {
    Fixture fixture = new Fixture();
    fixture.account.setChain(-3);

    assertThat(fixture.verify()).isFalse();
    assertThat(fixture.store.calls).hasValue(0);
    fixture.account.setChain(CHAIN);
    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void rejectsAnotherPayloadWithoutConsumingChallenge() throws Exception {
    Fixture fixture = new Fixture();
    fixture.proof.setPayload("another-server-challenge");
    fixture.sign();

    assertThat(fixture.verify()).isFalse();
    assertThat(fixture.store.calls).hasValue(0);
    fixture.proof.setPayload(fixture.challenge);
    fixture.sign();
    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void rejectsExpiredAndFutureProofsWithoutConsumingChallenge() throws Exception {
    for (long timestamp :
        new long[] {
          NOW - MAX_AGE - 1, NOW + FUTURE_SKEW + 1, Long.MIN_VALUE, Long.MAX_VALUE
        }) {
      Fixture fixture = new Fixture();
      fixture.proof.setTimestamp(timestamp);
      fixture.sign();

      assertThat(fixture.verify()).as("timestamp %s", timestamp).isFalse();
      assertThat(fixture.store.calls).hasValue(0);
      fixture.proof.setTimestamp(NOW);
      fixture.sign();
      assertThat(fixture.verify()).isTrue();
    }
  }

  @Test
  public void acceptsFreshnessWindowBoundaries() throws Exception {
    for (long timestamp : new long[] {NOW - MAX_AGE, NOW + FUTURE_SKEW}) {
      Fixture fixture = new Fixture();
      fixture.proof.setTimestamp(timestamp);
      fixture.sign();

      assertThat(fixture.verify()).as("timestamp %s", timestamp).isTrue();
    }
  }

  @Test
  public void zeroAgeAndSkewPermitOnlyCurrentTimestamp() throws Exception {
    Fixture fixture = new Fixture();
    ProofVerificationContext context =
        new ProofVerificationContext(DOMAIN, CHAIN, fixture.challenge, NOW, 0, 0);
    fixture.proof.setTimestamp(NOW - 1);
    fixture.sign();
    assertThat(TonConnect.verifyProof(fixture.proof, fixture.account, context, fixture.store))
        .isFalse();
    fixture.proof.setTimestamp(NOW + 1);
    fixture.sign();
    assertThat(TonConnect.verifyProof(fixture.proof, fixture.account, context, fixture.store))
        .isFalse();
    fixture.proof.setTimestamp(NOW);
    fixture.sign();
    assertThat(TonConnect.verifyProof(fixture.proof, fixture.account, context, fixture.store))
        .isTrue();
  }

  @Test
  public void rejectsUnknownAndExpiredStoredChallenges() throws Exception {
    Fixture unknown = new Fixture();
    unknown.store.challenges.clear();
    assertThat(unknown.verify()).isFalse();

    Fixture expired = new Fixture();
    expired.store.issue(expired.challenge, ADDRESS, DOMAIN, CHAIN, NOW);
    assertThat(expired.verify()).isFalse();
  }

  @Test
  public void storeEnforcesAccountDomainAndChainBinding() throws Exception {
    Fixture wrongAccount = new Fixture();
    wrongAccount.store.issue(
        wrongAccount.challenge, "-1:" + ADDRESS.substring(2), DOMAIN, CHAIN, NOW + 60);
    assertThat(wrongAccount.verify()).isFalse();

    Fixture wrongDomain = new Fixture();
    wrongDomain.store.issue(wrongDomain.challenge, ADDRESS, "other.example", CHAIN, NOW + 60);
    assertThat(wrongDomain.verify()).isFalse();

    Fixture wrongChain = new Fixture();
    wrongChain.store.issue(wrongChain.challenge, ADDRESS, DOMAIN, -3, NOW + 60);
    assertThat(wrongChain.verify()).isFalse();
  }

  @Test
  public void challengeStoreFailureCannotAuthenticate() throws Exception {
    Fixture fixture = new Fixture();
    assertThat(
            TonConnect.verifyProof(
                fixture.proof,
                fixture.account,
                fixture.context(),
                (challenge, address, domain, chain, now) -> false))
        .isFalse();
    IOException failure = new IOException("challenge store unavailable");
    assertThat(
            assertThrows(
                IOException.class,
                () ->
                    TonConnect.verifyProof(
                        fixture.proof,
                        fixture.account,
                        fixture.context(),
                        (challenge, address, domain, chain, now) -> {
                          throw failure;
                        })))
        .isSameAs(failure);
    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void invalidSignatureCannotBurnChallenge() throws Exception {
    Fixture fixture = new Fixture();
    fixture.proof.setSignature(Utils.bytesToBase64SafeUrl(new byte[64]));

    assertThat(fixture.verify()).isFalse();
    assertThat(fixture.store.calls).hasValue(0);
    fixture.sign();
    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void rejectsMalformedFieldsWithoutConsumingChallenge() throws Exception {
    Fixture fixture = new Fixture();
    assertThat(TonConnect.verifyProof(null, fixture.account, fixture.context(), fixture.store))
        .isFalse();
    assertThat(TonConnect.verifyProof(fixture.proof, null, fixture.context(), fixture.store))
        .isFalse();
    fixture.proof.setDomain(null);
    assertThat(fixture.verify()).isFalse();
    fixture.proof.setDomain(domain(null));
    assertThat(fixture.verify()).isFalse();
    fixture.proof.setDomain(domain(""));
    assertThat(fixture.verify()).isFalse();
    fixture.proof.setDomain(domain(DOMAIN));
    fixture.proof.getDomain().setLengthBytes(DOMAIN.length() + 1);
    fixture.sign();
    assertThat(fixture.verify()).isFalse();
    fixture.proof.setDomain(domain(DOMAIN));
    fixture.proof.setPayload(null);
    assertThat(fixture.verify()).isFalse();
    fixture.proof.setPayload("");
    assertThat(fixture.verify()).isFalse();
    fixture.proof.setPayload(fixture.challenge);
    for (String signature :
        new String[] {null, "", "%%%", Utils.bytesToBase64SafeUrl(new byte[63])}) {
      fixture.proof.setSignature(signature);
      assertThat(fixture.verify()).isFalse();
    }
    fixture.sign();
    String publicKey = fixture.account.getPublicKey();
    fixture.account.setPublicKey("invalid hex");
    assertThat(fixture.verify()).isFalse();
    fixture.account.setPublicKey("00");
    assertThat(fixture.verify()).isFalse();
    fixture.account.setPublicKey(publicKey);
    fixture.account.setAddress("not-an-address");
    assertThat(fixture.verify()).isFalse();
    fixture.account.setAddress(null);
    assertThat(fixture.verify()).isFalse();
    fixture.account.setAddress(ADDRESS);
    assertThat(fixture.store.calls).hasValue(0);
    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void rejectsMalformedDomainForms() throws Exception {
    for (String value :
        new String[] {
          "https://login.example", "login.example:443", "login.example/path", "login.example.",
          " login.example", "login..example", "-login.example", "login-.example", "lögin.example"
        }) {
      Fixture fixture = new Fixture();
      fixture.proof.setDomain(domain(value));
      fixture.sign();

      assertThat(fixture.verify()).as("domain %s", value).isFalse();
      assertThat(fixture.store.calls).hasValue(0);
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new ProofVerificationContext(
                  value, CHAIN, fixture.challenge, NOW, MAX_AGE, FUTURE_SKEW));
    }
  }

  @Test
  public void rejectsInvalidVerifierConfiguration() {
    for (String domain : new String[] {null, ""}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new ProofVerificationContext(domain, CHAIN, "challenge", NOW, MAX_AGE, FUTURE_SKEW));
    }
    for (String challenge : new String[] {null, ""}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProofVerificationContext(DOMAIN, CHAIN, challenge, NOW, MAX_AGE, FUTURE_SKEW));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProofVerificationContext(DOMAIN, CHAIN, "challenge", -1, MAX_AGE, FUTURE_SKEW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProofVerificationContext(DOMAIN, CHAIN, "challenge", NOW, -1, FUTURE_SKEW));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProofVerificationContext(DOMAIN, CHAIN, "challenge", NOW, MAX_AGE, -1));
  }

  @Test
  public void messageEncodingMatchesIndependentUnicodeAndWorkchainVector() throws Exception {
    TonProof proof =
        TonProof.builder()
            .timestamp(1722999580)
            .domain(domain("example.com"))
            .payload("login:你好 🔑")
            .build();
    String address = "-1:" + ADDRESS.substring(2);
    byte[] message = TonConnect.createMessageForSigning(proof, address);

    // Computed from the protocol fields with Python hashlib and struct, independently of ton4j.
    assertThat(Utils.bytesToHex(message))
        .isEqualTo("faccaa91bc367573824db8bc192205c1702b03654672764e60a6697b4d822a08");
    assertThat(message).isEqualTo(referenceMessage(proof, address));
    assertThat(
            Utils.bytesToHex(
                TonConnect.createMessageForSigning(proof, "1:" + ADDRESS.substring(2))))
        .isEqualTo("7c3d01dc553fa33fb703dd72bdfbd2231bd04daa1cc5aebdb7f5d4c13de6c861");
  }

  @Test
  public void messageEncodingPreservesAsciiWorkchainZeroVector() throws Exception {
    TonProof proof =
        TonProof.builder()
            .timestamp(1722999580)
            .domain(domain("example.com"))
            .payload("login-challenge")
            .build();

    assertThat(Utils.bytesToHex(TonConnect.createMessageForSigning(proof, ADDRESS)))
        .isEqualTo("23279b574ba9529e9f6770f2a8c5ad65a9501725e591b2a0f1be632483c61a7b");
  }

  @Test
  @SuppressWarnings("deprecation")
  public void signatureOnlyApiRemainsCompatibleButDoesNotAuthenticateContext() throws Exception {
    Fixture fixture = new Fixture();
    fixture.proof.setDomain(domain("other.example"));
    fixture.proof.setTimestamp(NOW - MAX_AGE - 1);
    fixture.sign();

    assertThat(TonConnect.verifyProofSignature(fixture.proof, fixture.account)).isTrue();
    assertThat(TonConnect.checkProof(fixture.proof, fixture.account)).isTrue();
    assertThat(fixture.verify()).isFalse();
  }

  private static Domain domain(String value) {
    return Domain.builder()
        .value(value)
        .lengthBytes(value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length)
        .build();
  }

  private static byte[] referenceMessage(TonProof proof, String address) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream message = new DataOutputStream(bytes);
    message.write("ton-proof-item-v2/".getBytes(StandardCharsets.UTF_8));
    String[] addressParts = address.split(":", -1);
    message.writeInt(Integer.parseInt(addressParts[0]));
    message.write(Utils.hexToSignedBytes(addressParts[1]));
    writeLittleEndian(message, proof.getDomain().getLengthBytes(), 4);
    message.write(proof.getDomain().getValue().getBytes(StandardCharsets.UTF_8));
    writeLittleEndian(message, proof.getTimestamp(), 8);
    message.write(proof.getPayload().getBytes(StandardCharsets.UTF_8));
    byte[] firstHash = MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
    bytes.reset();
    message.writeByte(0xff);
    message.writeByte(0xff);
    message.write("ton-connect".getBytes(StandardCharsets.UTF_8));
    message.write(firstHash);
    return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
  }

  private static void writeLittleEndian(DataOutputStream output, long value, int length)
      throws Exception {
    for (int i = 0; i < length; i++) {
      output.writeByte((int) (value >>> (8 * i)) & 0xff);
    }
  }

  private static class Fixture {
    final TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair();
    final String challenge = UUID.randomUUID().toString();
    final MemoryChallengeStore store = new MemoryChallengeStore();
    final WalletAccount account =
        WalletAccount.builder()
            .address(ADDRESS)
            .chain(CHAIN)
            .publicKey(Utils.bytesToHex(keyPair.getPublicKey()))
            .build();
    final TonProof proof =
        TonProof.builder().timestamp(NOW).domain(domain(DOMAIN)).payload(challenge).build();

    Fixture() throws Exception {
      store.issue(challenge, ADDRESS, DOMAIN, CHAIN, NOW + 60);
      sign();
    }

    void sign() throws Exception {
      proof.setSignature(
          Utils.bytesToBase64SafeUrl(
              Utils.signData(
                  keyPair.getPublicKey(),
                  keyPair.getSecretKey(),
                  referenceMessage(proof, account.getAddress()))));
    }

    ProofVerificationContext context() {
      return new ProofVerificationContext(DOMAIN, CHAIN, challenge, NOW, MAX_AGE, FUTURE_SKEW);
    }

    boolean verify() throws Exception {
      return TonConnect.verifyProof(proof, account, context(), store);
    }
  }

  private static class MemoryChallengeStore implements ProofChallengeStore {
    final Map<String, IssuedChallenge> challenges = new ConcurrentHashMap<>();
    final AtomicInteger calls = new AtomicInteger();

    void issue(String challenge, String address, String domain, int chain, long expiresAt) {
      challenges.put(challenge, new IssuedChallenge(address, domain, chain, expiresAt));
    }

    @Override
    public boolean consume(String challenge, String address, String domain, int chain, long now) {
      calls.incrementAndGet();
      IssuedChallenge issued = challenges.get(challenge);
      return issued != null
          && issued.expiresAt > now
          && issued.address.equals(address)
          && issued.domain.equals(domain)
          && issued.chain == chain
          && challenges.remove(challenge, issued);
    }
  }

  private static class IssuedChallenge {
    final String address;
    final String domain;
    final int chain;
    final long expiresAt;

    IssuedChallenge(String address, String domain, int chain, long expiresAt) {
      this.address = address;
      this.domain = domain;
      this.chain = chain;
      this.expiresAt = expiresAt;
    }
  }
}
