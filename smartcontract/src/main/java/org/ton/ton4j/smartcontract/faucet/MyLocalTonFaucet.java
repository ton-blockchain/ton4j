package org.ton.ton4j.smartcontract.faucet;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.ContractUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

import java.math.BigInteger;

import static java.util.Objects.isNull;

@Slf4j
public class MyLocalTonFaucet {

  public static String SECRET_KEY = "a51e8fb6f0fae3834bf430f5012589d319e7b3b3303ceb82c816b762fccf2d05";

  public static String FAUCET_ADDRESS_RAW =
          "-1:22f53b7d9aba2cef44755f7078b01614cd4dde2388a1729c2c386cf8f9898afe";

  static TweetNaclFast.Signature.KeyPair keyPair =
          Utils.generateSignatureKeyPairFromSeed(Utils.hexToSignedBytes(SECRET_KEY));


  public static BigInteger getBalance(TonProvider tonProvider) {
    return WalletV3R2.builder()
        .tonProvider(tonProvider)
        .keyPair(keyPair)
        .wc(-1)
        .walletId(42)
        .build()
        .getBalance();
  }

  /**
   * @deprecated Use {@link #getBalance(TonProvider)}.
   */
  @Deprecated
  public static BigInteger getBalance(AdnlLiteClient adnlLiteClient) {
    return getBalance((TonProvider) adnlLiteClient);
  }

  public static BigInteger topUpContract(
      TonProvider tonProvider, Address destinationAddress, BigInteger amount) throws Exception {
    if (tonProvider instanceof Tonlib) {
      return topUpContract((Tonlib) tonProvider, destinationAddress, amount);
    }
    if (tonProvider instanceof AdnlLiteClient) {
      return topUpContract((AdnlLiteClient) tonProvider, destinationAddress, amount);
    }
    if (tonProvider instanceof TonCenter) {
      return topUpContract((TonCenter) tonProvider, destinationAddress, amount);
    }
    throw new Error(
        "Unsupported TonProvider implementation: "
            + (tonProvider == null ? "null" : tonProvider.getClass()));
  }

  /**
   * @deprecated Use {@link #topUpContract(TonProvider, Address, BigInteger)}.
   */
  @Deprecated
  public static BigInteger topUpContract(
      Tonlib tonlib, Address destinationAddress, BigInteger amount) throws InterruptedException {


    WalletV3R2 faucetWallet =
        WalletV3R2.builder().tonProvider(tonlib).keyPair(keyPair).wc(-1).walletId(42).build();

    BigInteger faucetBalance = null;
    int i = 0;
    do {
      try {
        if (i++ > 10) {
          throw new Error("Cannot get MyLocalTon faucet balance. Restart.");
        }

        faucetBalance = faucetWallet.getBalance();
        log.info(
            "MyLocalTon faucet address {}, balance {}",
            faucetWallet.getAddress().toRaw(),
            Utils.formatNanoValue(faucetBalance));
        if (faucetBalance.compareTo(amount) < 0) {
          throw new Error(
              "MyLocalTon faucet does not have that much toncoins. Faucet balance "
                  + Utils.formatNanoValue(faucetBalance)
                  + ", requested "
                  + Utils.formatNanoValue(amount));
        }
      } catch (Exception e) {
        log.info("Cannot get MyLocalTon faucet balance. Restarting...");
        Utils.sleep(5, "Waiting for MyLocalTon faucet balance");
      }
    } while (isNull(faucetBalance));

    WalletV3Config config =
        WalletV3Config.builder()
            .bounce(false)
            .walletId(42)
            .seqno(faucetWallet.getSeqno())
            .destination(destinationAddress)
            .amount(amount)
            .comment("top-up from ton4j MyLocalTon faucet")
            .build();

    SendResponse sendResponse = faucetWallet.send(config);

    if (sendResponse.getCode() != 0) {
      throw new Error(sendResponse.getMessage());
    }

    tonlib.waitForBalanceChange(destinationAddress, 60);

    return tonlib.getAccountBalance(destinationAddress);
  }

  /**
   * @deprecated Use {@link #topUpContract(TonProvider, Address, BigInteger)}.
   */
  @Deprecated
  public static BigInteger topUpContract(
      AdnlLiteClient adnlLiteClient, Address destinationAddress, BigInteger amount)
      throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(SECRET_KEY));

    WalletV3R2 faucet =
        WalletV3R2.builder()
            .adnlLiteClient(adnlLiteClient)
            .keyPair(keyPair)
            .wc(-1)
            .walletId(42)
            .build();

    BigInteger faucetBalance = null;
    int i = 0;
    do {
      try {
        if (i++ > 10) {
          throw new Error("Cannot get testnet faucet balance. Restart.");
        }

        faucetBalance = faucet.getBalance();
        log.info(
            "MyLocalTon faucet address {}, balance {}",
            faucet.getAddress().toRaw(),
            Utils.formatNanoValue(faucetBalance));
        if (faucetBalance.compareTo(amount) < 0) {
          throw new Error(
              "MyLocalTon faucet does not have that much toncoins. Faucet balance "
                  + Utils.formatNanoValue(faucetBalance)
                  + ", requested "
                  + Utils.formatNanoValue(amount));
        }
      } catch (Exception e) {
        log.info("Cannot get MyLocalTon faucet balance. Restarting...");
        Utils.sleep(5, "Waiting for MyLocalTon faucet balance");
      }
    } while (isNull(faucetBalance));

    WalletV3Config config =
        WalletV3Config.builder()
            .bounce(false)
            .walletId(42)
            .seqno(faucet.getSeqno())
            .destination(destinationAddress)
            .amount(amount)
            .comment("top-up from ton4j faucet")
            .build();

    SendResponse sendResponse = faucet.send(config);

    if (sendResponse.getCode() != 0) {
      throw new Error(sendResponse.getMessage());
    }

    adnlLiteClient.waitForBalanceChange(destinationAddress, 60);
    return adnlLiteClient.getBalance(destinationAddress);
  }

  private static BigInteger topUpContract(
      TonCenter tonCenterClient, Address destinationAddress, BigInteger amount) throws Exception {
    WalletV3R2 faucetWallet =
        WalletV3R2.builder().tonProvider(tonCenterClient).keyPair(keyPair).wc(-1).walletId(42).build();

    BigInteger faucetBalance = null;
    int i = 0;
    do {
      try {
        if (i++ > 10) {
          throw new Error("Cannot get MyLocalTon faucet balance. Restart.");
        }

        faucetBalance = faucetWallet.getBalance();
        log.info(
            "MyLocalTon faucet address {}, balance {}",
            faucetWallet.getAddress().toRaw(),
            Utils.formatNanoValue(faucetBalance));
        if (faucetBalance.compareTo(amount) < 0) {
          throw new Error(
              "MyLocalTon faucet does not have that much toncoins. Faucet balance "
                  + Utils.formatNanoValue(faucetBalance)
                  + ", requested "
                  + Utils.formatNanoValue(amount));
        }
      } catch (Exception e) {
        log.info("Cannot get MyLocalTon faucet balance. Restarting...");
        Utils.sleep(5, "Waiting for MyLocalTon faucet balance");
      }
    } while (isNull(faucetBalance));

    WalletV3Config config =
        WalletV3Config.builder()
            .bounce(false)
            .walletId(42)
            .seqno(faucetWallet.getSeqno())
            .destination(destinationAddress)
            .amount(amount)
            .comment("top-up from ton4j MyLocalTon faucet")
            .build();

    SendResponse sendResponse = faucetWallet.send(config);

    if (sendResponse.getCode() != 0) {
      throw new Error(sendResponse.getMessage());
    }

    BigInteger initialBalance = tonCenterClient.getBalance(destinationAddress.toBounceable());
    int timeoutSeconds = 60;
    int j = 0;
    BigInteger currentBalance;
    do {
      if (++j * 2 >= timeoutSeconds) {
        throw new Error("Balance was not changed within specified timeout.");
      }
      Utils.sleep(2);
      currentBalance = tonCenterClient.getBalance(destinationAddress.toBounceable());
    } while (initialBalance.equals(currentBalance));

    return currentBalance;
  }
}
