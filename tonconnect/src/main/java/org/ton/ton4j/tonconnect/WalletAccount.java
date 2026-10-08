package org.ton.ton4j.tonconnect;

import lombok.*;

/**
 * Untrusted account fields returned by a TonConnect wallet.
 *
 * <p>A submitted public key alone does not establish ownership of the address. Authenticate with
 * {@link TonConnect#verifyProof(TonProof, WalletAccount, ProofVerificationContext,
 * ProofChallengeStore)} to validate a supported StateInit, or with
 * {@link TonConnect#verifyProofWithTrustedKey(TonProof, WalletAccount, ProofVerificationContext,
 * ProofChallengeStore, TrustedWalletKeyResolver)} and a backend-owned trusted chain resolver.
 */
@Builder
@Data
public class WalletAccount {
  /** Claimed raw TON address in {@code workchain:hash} format. */
  private String address;

  /** Optional hexadecimal public key; authentication checks it against the independently bound key. */
  private String publicKey;

  /** Claimed TON network global ID; authentication compares it with the backend's expected chain. */
  private int chain;

  /** Base64 BoC of the wallet's initial StateInit, required for local key and address verification. */
  private String walletStateInit;
}
