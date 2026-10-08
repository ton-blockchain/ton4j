package org.ton.ton4j.tonconnect;

import static java.util.Objects.isNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.mnemonic.Ed25519;
import org.ton.ton4j.tlb.StateInit;
import org.ton.ton4j.utils.Utils;

public class TonConnect {

  // Unique prefix to separate messages from on-chain messages.
  public static final String TON_CONNECT = "ton-connect";

  /**
   * Checks the signature only. This does not authenticate a login or prevent replay.
   *
   * @deprecated Use {@link #verifyProof(TonProof, WalletAccount, ProofVerificationContext,
   *     ProofChallengeStore)} for backend proof verification.
   */
  @Deprecated
  public static boolean checkProof(TonProof tonProof, WalletAccount account) throws Exception {
    return verifyProofSignature(tonProof, account);
  }

  /**
   * Verify backend-owned context and the signature, then atomically consume the login challenge.
   *
   * <p>The backend must resolve and validate the account's public key against the address on the
   * expected chain before calling this method. A wallet-supplied public key alone is not trusted.
   * Context and challenge storage must come from the backend, not the submitted proof. A true result
   * is returned only after successful challenge consumption; issue a login session only then.
   *
   * @return false for invalid proofs or rejected challenges
   * @throws Exception if signature verification or challenge storage cannot be completed
   */
  public static boolean verifyProof(
      TonProof tonProof,
      WalletAccount account,
      ProofVerificationContext context,
      ProofChallengeStore challengeStore)
      throws Exception {
    Objects.requireNonNull(context, "Verification context is required");
    Objects.requireNonNull(challengeStore, "Challenge store is required");
    if (tonProof == null || account == null) {
      return false;
    }

    // Verify and consume the same values even if callers subsequently mutate their DTOs.
    Domain domain = tonProof.getDomain();
    if (domain == null) {
      return false;
    }
    TonProof proof =
        TonProof.builder()
            .timestamp(tonProof.getTimestamp())
            .domain(
                Domain.builder().value(domain.getValue()).lengthBytes(domain.getLengthBytes()).build())
            .payload(tonProof.getPayload())
            .signature(tonProof.getSignature())
            .build();
    WalletAccount wallet =
        WalletAccount.builder()
            .address(account.getAddress())
            .chain(account.getChain())
            .publicKey(account.getPublicKey())
            .walletStateInit(account.getWalletStateInit())
            .build();

    String normalizedDomain;
    try {
      normalizedDomain = ProofVerificationContext.normalizeDomain(proof.getDomain().getValue());
      wallet.setAddress(normalizeAddress(wallet.getAddress()));
      if (!context.getExpectedDomain().equals(normalizedDomain)
          || wallet.getChain() != context.getExpectedChain()
          || !context.getExpectedChallenge().equals(proof.getPayload())
          || proof.getDomain().getLengthBytes()
              != proof.getDomain().getValue().getBytes(StandardCharsets.UTF_8).length
          || proof.getTimestamp() < 0
          || proof.getSignature() == null) {
        return false;
      }
      long now = context.getNowEpochSeconds();
      long timestamp = proof.getTimestamp();
      // Subtract only nonnegative, ordered values to avoid overflow in age/skew checks.
      if ((timestamp <= now && now - timestamp > context.getMaxAgeSeconds())
          || (timestamp > now && timestamp - now > context.getAllowedFutureSkewSeconds())) {
        return false;
      }
      if (!verifyProofSignature(proof, wallet)) {
        return false;
      }
    } catch (DecoderException | RuntimeException invalidProof) {
      return false;
    }

    return challengeStore.consume(
        context.getExpectedChallenge(),
        wallet.getAddress(),
        normalizedDomain,
        context.getExpectedChain(),
        context.getNowEpochSeconds());
  }

  /**
   * Verify cryptographic signature validity only. No audience, chain, time or challenge checks are
   * performed, so this method alone must not be used to authenticate a login.
   */
  public static boolean verifyProofSignature(TonProof tonProof, WalletAccount account) throws Exception {
    byte[] publicKeyBytes;
    if (StringUtils.isEmpty(account.getPublicKey()) || isNull(account.getPublicKey())) {
      StateInit stateInit =
          StateInit.deserialize(
              CellSlice.beginParse(
                  CellBuilder.beginCell().fromBocBase64(account.getWalletStateInit()).endCell()));
      publicKeyBytes =
          CellSlice.beginParse(stateInit.getData()).skipBits(32).skipBits(32).loadBytes(256);
    } else {
      publicKeyBytes = Hex.decodeHex(account.getPublicKey());
    }
    byte[] signature = Utils.base64SafeUrlToBytes(tonProof.getSignature());
    if (publicKeyBytes.length != 32 || signature.length != 64) {
      return false;
    }
    byte[] messageForSigning = createMessageForSigning(tonProof, account.getAddress());

    return Ed25519.verify(publicKeyBytes, messageForSigning, signature);
  }

  /** message = utf8_encode("ton-proof-item-v2/") ++ Address ++ AppDomain ++ Timestamp ++ Payload */
  private static byte[] createMessage(TonProof tonProof, String address) throws DecoderException {
    String rawAddress = normalizeAddress(address);
    int separator = rawAddress.indexOf(':');
    int workchain = Integer.parseInt(rawAddress.substring(0, separator));
    byte[] addressBytes = Hex.decodeHex(rawAddress.substring(separator + 1));
    long timestamp = tonProof.getTimestamp();
    int domainLength = tonProof.getDomain().getLengthBytes();
    String domainValue = tonProof.getDomain().getValue();
    String payload = tonProof.getPayload();
    byte[] domainBytes = domainValue.getBytes(StandardCharsets.UTF_8);
    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

    // Create message
    byte[] proofItemPrefix = "ton-proof-item-v2/".getBytes(StandardCharsets.UTF_8);
    ByteBuffer messageBuffer =
        ByteBuffer.allocate(
            proofItemPrefix.length
                + 4
                + addressBytes.length
                + 4
                + domainBytes.length
                + 8
                + payloadBytes.length);
    messageBuffer.put(proofItemPrefix);
    messageBuffer.putInt(workchain); // workchain, big-endian
    messageBuffer.put(addressBytes);
    messageBuffer.putInt(Integer.reverseBytes(domainLength)); // domain length, little-endian
    messageBuffer.put(domainBytes);
    messageBuffer.putLong(Long.reverseBytes(timestamp)); // timestamp, little-endian
    messageBuffer.put(payloadBytes);

    return messageBuffer.array();
  }

  private static String normalizeAddress(String address) throws DecoderException {
    if (address == null) {
      throw new IllegalArgumentException("Account address is required");
    }
    String[] parts = address.split(":", -1);
    if (parts.length != 2 || parts[1].length() != 64) {
      throw new IllegalArgumentException("Expected a raw workchain:hash account address");
    }
    int workchain = Integer.parseInt(parts[0]);
    return workchain + ":" + Hex.encodeHexString(Hex.decodeHex(parts[1]));
  }

  /**
   * Create message for signing format: sha256( 0xffff ++ utf8_encode("ton-connect") ++
   * sha256(message) ) result sha256 of message for signing
   */
  public static byte[] createMessageForSigning(TonProof tonProof, String address)
      throws NoSuchAlgorithmException, DecoderException {
    byte[] message = createMessage(tonProof, address);
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] hashedMessage = sha256.digest(message);
    byte[] signatureMessage = new byte[2 + TON_CONNECT.length() + hashedMessage.length];
    signatureMessage[0] = (byte) 0xFF;
    signatureMessage[1] = (byte) 0xFF;
    System.arraycopy(
        TON_CONNECT.getBytes(StandardCharsets.UTF_8), 0, signatureMessage, 2, TON_CONNECT.length());
    System.arraycopy(
        hashedMessage, 0, signatureMessage, 2 + TON_CONNECT.length(), hashedMessage.length);

    return sha256.digest(signatureMessage);
  }
}
