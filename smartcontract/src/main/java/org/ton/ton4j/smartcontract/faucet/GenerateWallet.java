package org.ton.ton4j.smartcontract.faucet;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.smartcontract.highload.HighloadWalletV3;
import org.ton.ton4j.smartcontract.types.HighloadQueryId;
import org.ton.ton4j.smartcontract.types.HighloadV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class GenerateWallet {

  /**
   * Creates and tops up V3R1 wallet with initialBalanceInToncoins and walletId=42 in testnet
   *
   * @param tonProvider instance of Tonlib, AdnlLiteClient or TonCenter
   * @param initialBalanceInToncoins must not be zero
   * @return instance of WalletV3R1
   */
  public static WalletV3R1 randomV3R1(TonProvider tonProvider, long initialBalanceInToncoins) {
    log.info("generating WalletV3R1 wallet...");

    WalletV3R1 wallet = WalletV3R1.builder().tonProvider(tonProvider).wc(0).walletId(42).build();

    Address address = wallet.getAddress();

    String nonBounceableAddress = address.toNonBounceable();
    String bounceableAddress = address.toBounceable();
    String rawAddress = address.toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pubKey: {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));

    BigInteger balance;
    try {
      balance =
          TestnetFaucet.topUpContract(
              tonProvider,
              Address.of(nonBounceableAddress),
              Utils.toNano(initialBalanceInToncoins));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    log.info("new wallet balance {}", Utils.formatNanoValue(balance));
    wallet.deploy();
    wallet.waitForDeployment();

    return wallet;
  }

  /**
   * Creates and tops up V3R1 wallet with initialBalanceInToncoins and walletId=42 in testnet
   *
   * @param tonProvider instance of Tonlib, AdnlLiteClient or TonCenter
   * @param initialBalanceInToncoins must not be zero
   * @return instance of WalletV3R1
   */
  public static WalletV4R2 randomV4R2(TonProvider tonProvider, long initialBalanceInToncoins) {
    log.info("generating WalletV4R2 wallet...");

    WalletV4R2 wallet = WalletV4R2.builder().tonProvider(tonProvider).wc(0).walletId(42).build();

    Address address = wallet.getAddress();

    String nonBounceableAddress = address.toNonBounceable();
    String bounceableAddress = address.toBounceable();
    String rawAddress = address.toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);
    log.info("pubKey: {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));

    BigInteger balance;
    try {
      balance =
              TestnetFaucet.topUpContract(
                      tonProvider,
                      Address.of(nonBounceableAddress),
                      Utils.toNano(initialBalanceInToncoins));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    log.info("new wallet balance {}", Utils.formatNanoValue(balance));
    wallet.deploy();
    wallet.waitForDeployment();

    return wallet;
  }

  public static HighloadWalletV3 randomHighloadV3R1(
      TonProvider tonProvider, long initialBalanceInToncoins) {

    log.info("generating HighloadWalletV3 wallet...");
    HighloadWalletV3 wallet =
        HighloadWalletV3.builder().tonProvider(tonProvider).walletId(42).build();

    String nonBounceableAddress = wallet.getAddress().toNonBounceable();
    String bounceableAddress = wallet.getAddress().toBounceable();
    String rawAddress = wallet.getAddress().toRaw();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", rawAddress);

    log.info("pub-key {}", Utils.bytesToHex(wallet.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(wallet.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance;
    try {
      balance =
          TestnetFaucet.topUpContract(
              tonProvider,
              Address.of(nonBounceableAddress),
              Utils.toNano(initialBalanceInToncoins));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Utils.sleep(3, "topping up...");
    log.info("highload wallet {} balance: {}", wallet.getName(), Utils.formatNanoValue(balance));

    HighloadV3Config highloadV3Config =
        HighloadV3Config.builder()
            .walletId(42)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();

    wallet.deploy(highloadV3Config);
    wallet.waitForDeployment();
    return wallet;
  }
}
