package org.ton.ton4j.tonconnect;

import java.util.Locale;
import lombok.Getter;

/** Backend-owned expectations for one proof verification. All time values are Unix seconds. */
@Getter
public final class ProofVerificationContext {
  private final String expectedDomain;
  private final int expectedChain;
  private final String expectedChallenge;
  private final long nowEpochSeconds;
  private final long maxAgeSeconds;
  private final long allowedFutureSkewSeconds;

  /**
   * @param expectedDomain configured ASCII hostname, without a scheme, path, port or trailing dot;
   *     use ASCII punycode for internationalized names. Hostname case is normalized.
   * @param expectedChallenge unpredictable challenge retrieved from the backend's login session,
   *     never copied from the submitted proof
   * @param nowEpochSeconds current time supplied by the backend's clock
   * @param maxAgeSeconds maximum accepted proof age, inclusive
   * @param allowedFutureSkewSeconds maximum accepted future clock skew, inclusive
   * @throws IllegalArgumentException if the domain, challenge or time policy is invalid
   */
  public ProofVerificationContext(
      String expectedDomain,
      int expectedChain,
      String expectedChallenge,
      long nowEpochSeconds,
      long maxAgeSeconds,
      long allowedFutureSkewSeconds) {
    this.expectedDomain = normalizeDomain(expectedDomain);
    if (expectedChallenge == null || expectedChallenge.isEmpty()) {
      throw new IllegalArgumentException("An expected server-issued challenge is required");
    }
    if (nowEpochSeconds < 0 || maxAgeSeconds < 0 || allowedFutureSkewSeconds < 0) {
      throw new IllegalArgumentException("Current time, maximum age and future skew must be nonnegative");
    }
    this.expectedChain = expectedChain;
    this.expectedChallenge = expectedChallenge;
    this.nowEpochSeconds = nowEpochSeconds;
    this.maxAgeSeconds = maxAgeSeconds;
    this.allowedFutureSkewSeconds = allowedFutureSkewSeconds;
  }

  static String normalizeDomain(String domain) {
    if (domain == null || domain.isEmpty() || domain.length() > 253) {
      throw new IllegalArgumentException("Expected an ASCII hostname");
    }
    for (String label : domain.split("\\.", -1)) {
      if (label.isEmpty()
          || label.length() > 63
          || label.charAt(0) == '-'
          || label.charAt(label.length() - 1) == '-') {
        throw new IllegalArgumentException("Invalid hostname label");
      }
      for (int i = 0; i < label.length(); i++) {
        char c = label.charAt(i);
        if (!(c >= 'a' && c <= 'z')
            && !(c >= 'A' && c <= 'Z')
            && !(c >= '0' && c <= '9')
            && c != '-') {
          throw new IllegalArgumentException("Expected an ASCII hostname");
        }
      }
    }
    return domain.toLowerCase(Locale.ROOT);
  }
}
