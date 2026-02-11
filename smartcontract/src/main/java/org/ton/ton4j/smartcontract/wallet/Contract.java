package org.ton.ton4j.smartcontract.wallet;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.provider.SendResponse;
import org.ton.ton4j.smartcontract.types.WalletConfig;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R1;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tlb.print.MessagePrintInfo;
import org.ton.ton4j.tlb.print.TransactionPrintInfo;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.model.TransactionResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.tonlib.types.ExtraCurrency;
import org.ton.ton4j.utils.Utils;

/** Interface for all smart contract objects in ton4j. */
public interface Contract {

  /**
   * @deprecated Use {@link #getTonProvider()}.
   */
  @Deprecated
  Tonlib getTonlib();

  /**
   * @deprecated Use {@link #getTonProvider()}.
   */
  @Deprecated
  AdnlLiteClient getAdnlLiteClient();

  /**
   * @deprecated Use {@link #getTonProvider()}.
   */
  @Deprecated
  TonCenter getTonCenterClient();

  /**
   * Used for late tonlib assignment
   *
   * @param pTonlib Tonlib instance
   */
  /**
   * @deprecated Use {@link #setTonProvider(TonProvider)}.
   */
  @Deprecated
  void setTonlib(Tonlib pTonlib);

  /**
   * @deprecated Use {@link #setTonProvider(TonProvider)}.
   */
  @Deprecated
  void setAdnlLiteClient(AdnlLiteClient pAdnlClient);

  /**
   * @deprecated Use {@link #setTonProvider(TonProvider)}.
   */
  @Deprecated
  void setTonCenterClient(TonCenter pTonCenterClient);

  /**
   * @return active provider instance (Tonlib, AdnlLiteClient, or TonCenter)
   */
  default TonProvider getTonProvider() {
    if (nonNull(getTonCenterClient())) {
      return getTonCenterClient();
    }
    if (nonNull(getAdnlLiteClient())) {
      return getAdnlLiteClient();
    }
    return getTonlib();
  }

  /**
   * Used for late provider assignment.
   *
   * @param pTonProvider provider instance
   */
  default void setTonProvider(TonProvider pTonProvider) {
    if (pTonProvider instanceof TonCenter) {
      setTonCenterClient((TonCenter) pTonProvider);
      setAdnlLiteClient(null);
      setTonlib(null);
    } else if (pTonProvider instanceof AdnlLiteClient) {
      setAdnlLiteClient((AdnlLiteClient) pTonProvider);
      setTonCenterClient(null);
      setTonlib(null);
    } else if (pTonProvider instanceof Tonlib) {
      setTonlib((Tonlib) pTonProvider);
      setTonCenterClient(null);
      setAdnlLiteClient(null);
    } else if (pTonProvider == null) {
      setTonCenterClient(null);
      setAdnlLiteClient(null);
      setTonlib(null);
    } else {
      throw new Error("Unsupported TonProvider implementation: " + pTonProvider.getClass());
    }
  }

  long getWorkchain();

  String getName();

  default BigInteger getInitialBalance() {
    return null;
  }

  default Address getAddress() {
    return StateInit.builder()
        .code(createCodeCell())
        .data(createDataCell())
        .build()
        .getAddress(getWorkchain());
  }

  default Address getAddress(byte workchain) {
    return getStateInit().getAddress(workchain);
  }

  default MsgAddressIntStd getAddressIntStd() {
    Address ownAddress = getStateInit().getAddress(getWorkchain());
    return MsgAddressIntStd.builder()
        .workchainId(ownAddress.wc)
        .address(ownAddress.toBigInteger())
        .build();
  }

  default MsgAddressIntStd getAddressIntStd(int workchain) {
    Address ownAddress = getStateInit().getAddress();
    return MsgAddressIntStd.builder()
        .workchainId((byte) workchain)
        .address(ownAddress.toBigInteger())
        .build();
  }

  /**
   * @return Cell containing contact code
   */
  Cell createCodeCell();

  /**
   * Method to override
   *
   * @return {Cell} cell contains contract data
   */
  Cell createDataCell();

  default Cell createLibraryCell() {
    return null;
  }

  /**
   * Message StateInit consists of initial contract code, data and address in a blockchain
   *
   * @return StateInit
   */
  default StateInit getStateInit() {
    return StateInit.builder()
        .code(createCodeCell())
        .data(createDataCell())
        .lib(createLibraryCell())
        .build();
  }

  default long getSeqno() {

    if (this instanceof WalletV1R1) {
      throw new Error("Wallet V1R1 does not have seqno method");
    }
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        return ((TonCenter) provider).getSeqno(getAddress().toBounceable());
      } catch (Throwable e) {
        throw new Error(e);
      }
    }
    if (provider instanceof AdnlLiteClient) {
      try {
        return ((AdnlLiteClient) provider).getSeqno(getAddress());
      } catch (Exception e) {
        throw new Error(e);
      }
    }
    if (provider instanceof Tonlib) {
      return ((Tonlib) provider).getSeqno(getAddress());
    }
    throw new Error("Provider not set");
  }

  default boolean isDeployed() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        String state = ((TonCenter) provider).getState(getAddress().toBounceable());
        return "active".equals(state);
      } catch (Exception e) {
        return false;
      }
    } else if (provider instanceof AdnlLiteClient) {
      try {
        return (((AdnlLiteClient) provider)
                .getAccount(getAddress())
                .getAccountStorage()
                .getAccountState()
            instanceof AccountStateActive);
      } catch (Exception e) {
        return false;
      }
    } else if (provider instanceof Tonlib) {
      try {
        return StringUtils.isNotEmpty(
            ((Tonlib) provider).getRawAccountState(getAddress()).getCode());
      } catch (Exception e) {
        return false;
      }
    } else {
      throw new Error("Provider not set");
    }
  }

  default SendResponse send(Message externalMessage) {
    TonProvider provider = getTonProvider();
    if (provider == null) {
      throw new Error("Provider not set");
    }
    return provider.sendExternalMessage(externalMessage);
  }

  default Transaction sendWithConfirmation(Message externalMessage) throws Exception {
    TonProvider provider = getTonProvider();
    if (provider == null) {
      throw new Error("Provider not set");
    }
    return provider.sendExternalMessageWithConfirmation(externalMessage);
  }

  /** Checks every second for 60 seconds if the account state was deployed at the address */
  default void waitForDeployment() {
    waitForDeployment(60);
  }

  /** Checks every second for timeoutSeconds if the account state was deployed at the address */
  default void waitForDeployment(int timeoutSeconds) {
    int i = 0;
    do {
      if (++i >= timeoutSeconds) {
        throw new Error("Can't deploy contract within specified timeout.");
      }
      Utils.sleep(1);
    } while (!isDeployed());
  }

  /** Checks every second for 60 if the account balance was changed */
  default void waitForBalanceChange() {
    waitForBalanceChange(60);
  }

  /**
   * Checks every second for timeoutSeconds if account balance was changed. Notice, storage fee
   * often changes by 1 nanocoin with a few seconds, if you need to tolerate that consider using
   * waitForBalanceChangeWithTolerance().
   */
  default void waitForBalanceChange(int timeoutSeconds) {
    BigInteger initialBalance = getBalance();
    BigInteger currentBalance;
    int i = 0;
    do {
      if (++i >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(1);
      currentBalance = getBalance();

    } while (initialBalance.equals(currentBalance));
  }

  /**
   * Checks every second for 60 seconds if an account balance was changed with comparison to the initial balance. Notice, the storage fee
   * often changes by 1 nanocoin with a few seconds, if you need to tolerate that consider using
   * waitForBalanceChangeWithTolerance().
   */
  default void waitForBalanceChange(BigInteger initialBalance) {
    waitForBalanceChange(initialBalance, 60);
  }

  /**
   * Checks every second for timeoutSeconds if the account balance was changed with comparison to the initial balance. Notice, the storage fee
   * often changes by 1 nanocoin with a few seconds, if you need to tolerate that consider using
   * waitForBalanceChangeWithTolerance().
   */
  default void waitForBalanceChange(BigInteger initialBalance, int timeoutSeconds) {
    BigInteger currentBalance;
    int i = 0;
    do {
      if (++i >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(1);
      currentBalance = getBalance();

    } while (initialBalance.equals(currentBalance));
  }

  /**
   * returns if balance has changed by +/- tolerateNanoCoins within 60 seconds, otherwise throws an
   * error.
   *
   * @param tolerateNanoCoins tolerate value
   */
  default void waitForBalanceChangeWithTolerance(BigInteger tolerateNanoCoins) {
    waitForBalanceChangeWithTolerance(60, tolerateNanoCoins);
  }

  /**
   * returns if balance has changed by +/- tolerateNanoCoins within timeoutSeconds, otherwise throws
   * an error.
   *
   * @param timeoutSeconds timeout in seconds
   * @param tolerateNanoCoins tolerate value
   */
  default void waitForBalanceChangeWithTolerance(int timeoutSeconds, BigInteger tolerateNanoCoins) {

    BigInteger initialBalance = getBalance();
    long diff;
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(2);
      BigInteger currentBalance = getBalance();

      diff =
          Math.max(currentBalance.longValue(), initialBalance.longValue())
              - Math.min(currentBalance.longValue(), initialBalance.longValue());
    } while (diff < tolerateNanoCoins.longValue());
  }

  default BigInteger getBalance() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        return ((TonCenter) provider).getBalance(getAddress().toBounceable());
      } catch (Throwable e) {
        throw new Error(e);
      }
    } else if (provider instanceof AdnlLiteClient) {
      try {
        return ((AdnlLiteClient) provider).getBalance(getAddress());
      } catch (Exception e) {
        throw new Error(e);
      }
    } else if (provider instanceof Tonlib) {
      try {
        return new BigInteger(
            ((Tonlib) provider).getRawAccountState(getAddress()).getBalance());
      } catch (Exception e) {
        throw new Error(e);
      }
    } else {
      throw new Error("Provider not set");
    }
  }

  default void printTransactions() {
    printTransactions(20, false);
  }

  default void printTransactions(int historyLimit) {
    printTransactions(20, false);
  }

  /**
   * prints last 20 transactions and their messages if withMessages = true
   *
   * @param withMessages whether to print messages too
   */
  default void printTransactions(boolean withMessages) {
    printTransactions(20, withMessages);
  }

  /**
   * prints last historyLimit transactions and their messages if withMessages = true
   *
   * @param historyLimit amount of tx to show
   * @param withMessages whether to print messages too
   */
  default void printTransactions(int historyLimit, boolean withMessages) {
    List<Transaction> txs = getTransactionsTlb(0, historyLimit);
    TransactionPrintInfo.printTxHeader();
    for (Transaction tx : txs) {
      TransactionPrintInfo.printTransactionInfo(tx);
      if (withMessages) {
        TransactionPrintInfo.printAllMessages(tx, true, true);
      }
    }
    TransactionPrintInfo.printTxFooter();
  }

  default List<RawTransaction> getTransactions(int historyLimit) {
    return getTonlib()
        .getAllRawTransactions(getAddress().toBounceable(), BigInteger.ZERO, null, historyLimit)
        .getTransactions();
  }

  default List<RawTransaction> getTransactions() {
    return getTonlib()
        .getAllRawTransactions(getAddress().toBounceable(), BigInteger.ZERO, null, 20)
        .getTransactions();
  }

  default List<Transaction> getTransactionsTlb(int lt, byte[] hash, int historyLimit) {
    TonProvider provider = getTonProvider();
    if (provider instanceof AdnlLiteClient) {
      try {
        return ((AdnlLiteClient) provider)
            .getTransactions(getAddress(), lt, hash, historyLimit)
            .getTransactionsParsed();
      } catch (Exception e) {
        throw new Error(e);
      }
    } else if (provider instanceof TonCenter) {
      List<TransactionResponse> response =
          ((TonCenter) provider)
              .getTransactions(getAddress().toBounceable(), historyLimit)
              .getResult();
      List<Transaction> result = new ArrayList<>();
      for (TransactionResponse transactionResponse : response) {
        result.add(
            Transaction.deserialize(
                CellSlice.beginParse(Cell.fromBocBase64(transactionResponse.getData()))));
      }
      return result;
    } else if (provider instanceof Tonlib) {
      List<RawTransaction> response =
          ((Tonlib) provider)
              .getRawTransactionsV2(getAddress().toBounceable(), null, null, historyLimit, false)
              .getTransactions();
      List<Transaction> result = new ArrayList<>();
      for (RawTransaction tx : response) {
        result.add(Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(tx.getData()))));
      }
      return result;
    } else {
      throw new Error("Provider not set");
    }
  }

  default List<Transaction> getTransactionsTlb(int lt, int historyLimit) {
    return getTransactionsTlb(lt, null, historyLimit);
  }

  default List<Transaction> getTransactionsTlb(int historyLimit) {
    return getTransactionsTlb(0, null, historyLimit);
  }

  default List<Transaction> getTransactionsTlb() {
    return getTransactionsTlb(0, null, 20);
  }

  /** prints messages of last 20 transactions */
  default void printMessages() {
    printMessages(20);
  }

  /**
   * prints messages of last historyLimit transactions
   *
   * @param historyLimit amount of txs to print
   */
  default void printMessages(int historyLimit) {
    List<Transaction> txs = getTransactionsTlb(historyLimit);
    boolean first = true;
    for (Transaction tx : txs) {
      TransactionPrintInfo.printAllMessages(tx, first, false);
      first = false;
    }
    MessagePrintInfo.printMessageInfoFooter();
  }

  default Message prepareDeployMsg() {
    throw new Error("not implemented");
  }

  default Message prepareExternalMsg(WalletConfig config) {
    throw new Error("not implemented");
  }

  default BigInteger getGasFees() {
    switch (getName()) {
      case "V1R1":
        return BigInteger.valueOf(40000); // 0.00004 toncoins
      case "V1R2":
        return BigInteger.valueOf(40000);
      case "V1R3":
        return BigInteger.valueOf(40000);
      case "V2R1":
        return BigInteger.valueOf(40000);
      case "V2R2":
        return BigInteger.valueOf(40000);
      case "V3R1":
        return BigInteger.valueOf(40000);
      case "V3R2":
        return BigInteger.valueOf(40000);
      case "V4R2":
        return BigInteger.valueOf(310000);
      default:
        throw new Error("Unknown wallet version");
    }
  }

  default TonHashMapE convertExtraCurrenciesToHashMap(List<ExtraCurrency> extraCurrencies) {

    if (isNull(extraCurrencies)) {
      return null;
    }
    TonHashMapE x = new TonHashMapE(32);

    for (ExtraCurrency ec : extraCurrencies) {
      x.elements.put(ec.getId(), ec.getAmount());
    }
    return x;
  }

  default RawTransaction waitForExtraCurrency(long extraCurrencyId) {
    return waitForExtraCurrency(extraCurrencyId, null, null, 10);
  }

  default RawTransaction waitForExtraCurrency(
      long extraCurrencyId, BigInteger fromTxLt, String fromTxHash) {
    return waitForExtraCurrency(extraCurrencyId, fromTxLt, fromTxHash, 10);
  }

  /**
   * @param extraCurrencyId custom specified extra-currency id
   * @param attempts number of attempts to wait for tx with EC, an attempt runs every 5 sec.
   * @return RawTransaction if found
   */
  default RawTransaction waitForExtraCurrency(
      long extraCurrencyId, BigInteger fromTxLt, String fromTxHash, int attempts) {
    // todo
    for (int i = 0; i < attempts; i++) {
      RawTransactions txs =
          getTonlib().getRawTransactions(getAddress().toRaw(), fromTxLt, fromTxHash);
      for (RawTransaction tx : txs.getTransactions()) {
        for (ExtraCurrency ec : tx.getIn_msg().getExtra_currencies()) {
          if (ec.getId() == extraCurrencyId) {
            Transaction tlbTransaction = tx.getTransactionAsTlb();
            if (tlbTransaction.getDescription() instanceof TransactionDescriptionOrdinary) {
              BouncePhase bouncePhase = tlbTransaction.getOrdinaryTransaction().getBouncePhase();
              if (isNull(bouncePhase)) {
                return tx;
              } else {
                if (bouncePhase instanceof BouncePhaseOk) {
                  throw new Error("extra-currency transaction has bounced");
                } else {
                  return tx;
                }
              }
            } else {
              return tx;
            }
          }
        }
      }
      Utils.sleepMs(1000);
    }
    throw new Error("time out waiting for extra-currency with id " + extraCurrencyId);
  }
}
