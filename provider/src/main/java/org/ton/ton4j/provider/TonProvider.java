package org.ton.ton4j.provider;

import java.math.BigInteger;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tlb.Message;
import org.ton.ton4j.tlb.Transaction;

/** Common provider interface for TON blockchain clients. */
public interface TonProvider {

  BigInteger getBalance(Address address);

  long getSeqno(Address address);

  BigInteger getPublicKey(Address address);

  long getSubWalletId(Address address);

  boolean isDeployed(Address address);

  void waitForDeployment(Address address, int timeoutSeconds);

  void waitForBalanceChange(Address address, int timeoutSeconds);

  /** prints messages of an account's last 20 transactions */
  void printAccountMessages(Address account);

  /** prints messages of an account's last historyLimit transactions */
  void printAccountMessages(Address account, int historyLimit);

  /** prints transactions of an account's last 20 transactions */
  void printAccountTransactions(Address account);

  /** prints transactions of an account's last historyLimit transactions */
  void printAccountTransactions(Address account, int historyLimit);

  /** transactions with messages optionally */
  void printAccountTransactions(Address account, int historyLimit, boolean withMessages);

  /**
   * sends an external message and wait till the message's normalized hash found among account's
   * recent transactions
   */
  Transaction sendExternalMessageWithConfirmation(Message externalMessage);

  /**
   * sends an external message without waiting for a confirmation
   *
   * @return SendResponse with provider-specific status code and message
   */
  SendResponse sendExternalMessage(Message externalMessage);
}
