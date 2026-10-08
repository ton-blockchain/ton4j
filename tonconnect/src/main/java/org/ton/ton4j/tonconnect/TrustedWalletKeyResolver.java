package org.ton.ton4j.tonconnect;

/** Backend-owned public-key lookup for wallets that cannot be verified from a supported StateInit. */
@FunctionalInterface
public interface TrustedWalletKeyResolver {
  /**
   * Resolve the wallet's 32-byte Ed25519 public key for the supplied canonical raw address and chain.
   *
   * <p>The backend must obtain the key from a trusted, proof-verified chain source for this exact
   * account and chain. Never return the client's {@link WalletAccount#getPublicKey()} or extract a
   * key from unvalidated client state. Return null if the account or its public key cannot be
   * authenticated; lookup failures may throw. No successful authentication is possible without a
   * resolved key and a valid signature.
   */
  byte[] resolvePublicKey(String address, int chain) throws Exception;
}
