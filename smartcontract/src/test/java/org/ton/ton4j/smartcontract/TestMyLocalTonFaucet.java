package org.ton.ton4j.smartcontract;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.faucet.MyLocalTonFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R3;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestMyLocalTonFaucet {

  static String SECRET_KEY =
      "46aab91daaaa375d40588384fdf7e36c62d0c0f38c46adfea7f9c904c5973d97c02ece00eceb299066597ccc7a8ac0b2d08f0ad425f28c0ea92e74e2064f41f0";
  static String FAUCET_ADDRESS_RAW =
      "-1:22f53b7d9aba2cef44755f7078b01614cd4dde2388a1729c2c386cf8f9898afe";

  AdnlLiteClient adnlLiteClient =
          AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMyLocalTon()).build();

    public TestMyLocalTonFaucet() throws Exception {
    }

  @Test
  public void testFaucetBalance() {
    log.info("MyLocalTon faucet balance {} toncoins", Utils.formatNanoValue(MyLocalTonFaucet.getBalance(adnlLiteClient)));
  }

  @Test
  public void createFaucetWallet() {
    WalletV1R3 contract = WalletV1R3.builder().build();

    assertThat(contract.getAddress()).isNotNull();
    log.info("Private key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));
    log.info("Public key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info(
        "Non-bounceable address (for init): {}",
        contract.getAddress().toString(true, true, false, true));
    log.info(
        "Bounceable address (for later access): {}",
        contract.getAddress().toString(true, true, true, true));
    log.info("Raw address: {}", contract.getAddress().toString(false));
  }

  /**
   * Top up the non-bounceable address and then deploy the faucet. Update SECRET_KEY and
   * FAUCET_ADDRESS_RAW variables
   */
  @Test
  public void deployFaucetWallet() {
    byte[] secretKey = Utils.hexToSignedBytes(SECRET_KEY);
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .ignoreCache(false)
            .verbosityLevel(VerbosityLevel.DEBUG)
            .build();

    WalletV1R3 contract = WalletV1R3.builder().tonProvider(tonlib).keyPair(keyPair).build();

    log.info("Private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
    log.info("Public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
    String nonBounceableAddress = contract.getAddress().toString(true, true, false, true);
    log.info("Non-bounceable address (for init): {}", nonBounceableAddress);
    log.info(
        "Bounceable address (for later access): {}",
        contract.getAddress().toString(true, true, true, true));
    log.info("Raw address: {}", contract.getAddress().toString(false));

    //        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);
    SendResponse sendResponse = contract.deploy();
    assertThat(sendResponse.getCode()).isZero();
  }

  @Test
  public void topUpAnyContract() throws Exception {
    BigInteger newBalance =
        MyLocalTonFaucet.topUpContract(
            adnlLiteClient,
            Address.of("Ef_lZ1T4NCb2mwkme9h2rJfESCE0W34ma9lWp7-_uY3zXDvq"),
            Utils.toNano(1));
    log.info("new balance " + Utils.formatNanoValue(newBalance));
  }
}
