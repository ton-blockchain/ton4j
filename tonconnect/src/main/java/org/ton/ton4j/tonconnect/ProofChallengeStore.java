package org.ton.ton4j.tonconnect;

/** Backend storage for server-issued, one-time login challenges. */
@FunctionalInterface
public interface ProofChallengeStore {
  /**
   * Atomically validate and consume an issued challenge after proof verification succeeds.
   *
   * <p>Return true only if the challenge was issued for the current login session, remains pending,
   * is bound to the supplied raw account address, normalized domain and chain, and has a stored
   * expiry strictly greater than {@code nowEpochSeconds}. The check and deletion must be one atomic
   * operation shared by all verifier instances. Unknown, expired, mismatched and previously consumed
   * challenges must return false. A challenge must be consumed globally, not separately per account.
   * Never insert a challenge received from a proof or use separate lookup and unconditional delete.
   * Conditional removal of an immutable, validated entry is sufficient for a single-process store.
   *
   * <p>The application issues unpredictable challenges, stores their bindings and expiry, and retains
   * the expected challenge in the backend login session. Store failures must throw or return false;
   * verification must never succeed on a storage failure.
   */
  boolean consume(
      String challenge, String address, String domain, int chain, long nowEpochSeconds)
      throws Exception;
}
