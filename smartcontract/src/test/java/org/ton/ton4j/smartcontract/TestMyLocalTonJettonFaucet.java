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
import org.ton.ton4j.smartcontract.faucet.MyLocalTonJettonFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.faucet.TestnetJettonFaucet;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.ContractUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RawAccountState;
import org.ton.ton4j.utils.Utils;

/** Faucet for NEOJ jettons. */
@Slf4j
@RunWith(JUnit4.class)
public class TestMyLocalTonJettonFaucet {

  static String ADMIN_WALLET_SECRET_KEY =
      "be0bbb1725807ec0df984702a32a143864418400d797a48e267a120c3dc5f8d0d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  public static String ADMIN_WALLET_ADDRESS =
      "0:98972d1ab4b86f6be34ad03d64bb5e2cb369f0d7b5e53f13348664672b893010";
  public static String FAUCET_MASTER_ADDRESS = "0:502e0f88ac0c2e0e56bfd51e2b828ebe20e32d8d80d4b8f7e7bd72bfeb9d59b9";

  AdnlLiteClient adnlLiteClient =
      AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMyLocalTon()).build();

  public TestMyLocalTonJettonFaucet() throws Exception {}

  @Test
  public void testJettonFaucetBalance() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().adnlLiteClient(adnlLiteClient).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .adnlLiteClient(adnlLiteClient)
            .customAddress(Address.of(FAUCET_MASTER_ADDRESS))
            .build();

    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());

    Account state = adnlLiteClient.getAccount(Address.of(FAUCET_MASTER_ADDRESS));

    log.info("TEST FAUCET BALANCE IN TONCOINS {}", Utils.formatNanoValue(state.getBalance(), 2));
    log.info(
        "TEST FAUCET BALANCE TOTAL SUPPLY: {}",
        Utils.formatJettonValue(jettonMinterWallet.getTotalSupply(), 2, 2));
    log.info(
        "TEST FAUCET ADMIN BALANCE in TONCOINS: {}",
        Utils.formatNanoValue(adminWallet.getBalance()));
    log.info(
        "TEST FAUCET ADMIN BALANCE in JETTONS: {}",
        Utils.formatJettonValue(adminJettonWallet.getBalance(), 2, 2));
  }

  @Test
  public void createJettonFaucetAdminWallet() {
    WalletV3R2 contract = WalletV3R2.builder().walletId(42).build();

    assertThat(contract.getAddress()).isNotNull();
    log.info("Private key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));
    log.info("Public key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("Non-bounceable address (for init): {}", contract.getAddress().toNonBounceable());
    log.info("Bounceable address (for later access): {}", contract.getAddress().toBounceable());
    log.info("Raw address: {}", contract.getAddress().toRaw());
  }


  @Test
  public void testDeployMyLocalTonJettonFaucet() throws Exception {
    MyLocalTonJettonFaucet.deploy(adnlLiteClient);
  }

  @Test
  public void topUpAnyContractWithNeoJettons() {
    BigInteger newBalance =
        MyLocalTonJettonFaucet.topUpContractWithNeoj(
            adnlLiteClient,
            Address.of("0:94aa09fe231de4bb384f02428a8aaa9741acec27df0add54828b8409dd94c60b"),
            BigInteger.valueOf(100));
    log.info("new balance {} NEOJ", newBalance);
  }

  @Test
  public void testJettonBalance() {
    log.info(
        "balance: {}",
        ContractUtils.getJettonBalance(
            adnlLiteClient,
            Address.of(FAUCET_MASTER_ADDRESS),
            Address.of("0:94aa09fe231de4bb384f02428a8aaa9741acec27df0add54828b8409dd94c60b")));
  }
}
