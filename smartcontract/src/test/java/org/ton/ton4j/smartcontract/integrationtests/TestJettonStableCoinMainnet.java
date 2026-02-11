package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.provider.SendResponse;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.smartcontract.token.ft.JettonMinterStableCoin;
import org.ton.ton4j.smartcontract.token.ft.JettonWalletStableCoin;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.types.WalletV4R2Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestJettonStableCoinMainnet {
  public static final String USDT_MASTER_WALLET =
      "EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs";

  @Ignore
  @Test
  public void testJettonStableCoin() throws Exception {
    // careful - mainnet
    TonProvider adnlLiteClient = AdnlLiteClient.builder().mainnet().build();

    Address usdtMasterAddress = Address.of(USDT_MASTER_WALLET);

    byte[] secretKey = Utils.hexToSignedBytes("your-hex-secret-key");

    // use when you have 64 bytes private key
    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);

    // use when you have 32 bytes private key
    // TweetNaclFast.Signature.KeyPair keyPair =
    // TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    // generate private key and get wallet address, that you top up later
    /*
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    */

    WalletV3R2 randomDestinationWallet =
        WalletV3R2.builder().keyPair(Utils.generateSignatureKeyPair()).walletId(42).build();

    // use your wallet
    WalletV3R2 myWallet =
        WalletV3R2.builder().tonProvider(adnlLiteClient).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = myWallet.getAddress().toNonBounceable();
    String bounceableAddress = myWallet.getAddress().toBounceable();
    String rawAddress = myWallet.getAddress().toRaw();

    log.info("non-bounceable address: {}", nonBounceableAddress);
    log.info("    bounceable address: {}", bounceableAddress);
    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(myWallet.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(myWallet.getKeyPair().getSecretKey()));

    BigInteger balance = adnlLiteClient.getBalance(Address.of(bounceableAddress));
    log.info("account balance {}", Utils.formatNanoValue(balance));

    // myWallet.deploy();
    // myWallet.waitForDeployment(30);

    // get usdt jetton master (minter) address
    JettonMinterStableCoin usdtMasterWallet =
        JettonMinterStableCoin.builder()
            .tonProvider(adnlLiteClient)
            .customAddress(usdtMasterAddress)
            .build();

    log.info(
        "usdt total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

    // get my JettonWallet the one that holds my jettons (USDT) tokens
    JettonWalletStableCoin myJettonWallet = usdtMasterWallet.getJettonWallet(myWallet.getAddress());
    log.info(
        "my jettonWallet balance: {}", Utils.formatJettonValue(myJettonWallet.getBalance(), 6, 2));

    // send my jettons to external address
    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(myWallet.getSeqno())
            .destination(myJettonWallet.getAddress())
            .amount(Utils.toNano(0.02))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    Utils.toUsdt(0.02), // 2 cents
                    randomDestinationWallet.getAddress(), // recipient
                    null, // response address
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift")) // forward payload
                )
            .build();
    SendResponse sendResponse = myWallet.send(walletV3Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(
        9, "transferring 0.02 USDT jettons to wallet " + randomDestinationWallet.getAddress());

    BigInteger balanceOfDestinationWallet =
        adnlLiteClient.getBalance(randomDestinationWallet.getAddress());
    log.info("balanceOfDestinationWallet in toncoins: {}", balanceOfDestinationWallet);
    assertThat(balanceOfDestinationWallet).isEqualTo(BigInteger.ONE);

    JettonWalletStableCoin randomJettonWallet =
        usdtMasterWallet.getJettonWallet(randomDestinationWallet.getAddress());
    BigInteger randomJettonWalletBalance = randomJettonWallet.getBalance();
    log.info(
        "randomJettonWallet balance in jettons: {}",
        Utils.formatJettonValue(randomJettonWalletBalance, 6, 2));
    assertThat(randomJettonWalletBalance).isEqualTo(BigInteger.valueOf(20000));
  }

  @Ignore
  @Test
  public void testJettonStableCoinV4R2() throws Exception {
    // careful - mainnet
    TonProvider adnlLiteClient = AdnlLiteClient.builder().mainnet().build();

    Address usdtMasterAddress = Address.of(USDT_MASTER_WALLET);

    // 64 bytes private key of your wallet
    byte[] secretKey = Utils.hexToSignedBytes("add");

    // use when you have 64 bytes private key
    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);

    // use when you have 32 bytes private key
    // TweetNaclFast.Signature.KeyPair keyPair =
    // TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    // generate a private key and get a wallet address that you top up later
    // TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 randomDestinationWallet =
        WalletV4R2.builder().keyPair(Utils.generateSignatureKeyPair()).walletId(42).build();

    // use your wallet
    WalletV4R2 myWallet =
        WalletV4R2.builder().tonProvider(adnlLiteClient).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = myWallet.getAddress().toNonBounceable();
    String bounceableAddress = myWallet.getAddress().toBounceable();
    String rawAddress = myWallet.getAddress().toRaw();

    log.info("non-bounceable address: {}", nonBounceableAddress);
    log.info("    bounceable address: {}", bounceableAddress);
    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(myWallet.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(myWallet.getKeyPair().getSecretKey()));

    BigInteger balance = adnlLiteClient.getBalance(Address.of(bounceableAddress));
    log.info("account balance {}", Utils.formatNanoValue(balance));

    //        myWallet.deploy();
    //        myWallet.waitForDeployment(90);

    // get usdt jetton master (minter) address
    JettonMinterStableCoin usdtMasterWallet =
        JettonMinterStableCoin.builder()
            .tonProvider(adnlLiteClient)
            .customAddress(usdtMasterAddress)
            .build();

    log.info(
        "usdt total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

    // get my JettonWallet the one that holds my jettons (USDT) tokens
    JettonWalletStableCoin myJettonWallet = usdtMasterWallet.getJettonWallet(myWallet.getAddress());
    log.info(
        "my jettonWallet balance: {}", Utils.formatJettonValue(myJettonWallet.getBalance(), 6, 2));

    // send my jettons to external address
    WalletV4R2Config walletV4Config =
        WalletV4R2Config.builder()
            .walletId(42)
            .seqno(myWallet.getSeqno())
            .destination(myJettonWallet.getAddress())
            .amount(Utils.toNano(0.02))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    BigInteger.valueOf(20000), // 2 cents
                    randomDestinationWallet.getAddress(), // recipient
                    null, // response address
                    BigInteger.ZERO, // forward amount
                    null) // forward payload
                )
            .build();
    SendResponse sendResponse = myWallet.send(walletV4Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(
        3, "transferring 0.02 USDT jettons to wallet " + randomDestinationWallet.getAddress());

    BigInteger balanceOfDestinationWallet =
        adnlLiteClient.getBalance(randomDestinationWallet.getAddress());
    log.info("balanceOfDestinationWallet in toncoins: {}", balanceOfDestinationWallet);

    JettonWalletStableCoin randomJettonWallet =
        usdtMasterWallet.getJettonWallet(randomDestinationWallet.getAddress());
    BigInteger randomJettonWalletBalance = randomJettonWallet.getBalance();
    log.info(
        "randomJettonWallet balance in jettons: {}",
        Utils.formatJettonValue(randomJettonWalletBalance, 6, 2));

    assertThat(balanceOfDestinationWallet).isEqualTo(BigInteger.ZERO);
    assertThat(randomJettonWalletBalance).isEqualTo(BigInteger.valueOf(20000));
  }

  @Test
  public void testJettonStableCoinV4R2AdnlLiteClient() throws Exception {
    // careful - mainnet
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlMainnetGithub())
            .liteServerIndex(0)
            .build();

    Address usdtMasterAddress = Address.of(USDT_MASTER_WALLET);

    // 64 bytes private key of your wallet
    byte[] secretKey = Utils.hexToSignedBytes("add");

    // use when you have 64 bytes private key
    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);

    // use when you have 32 bytes private key
    // TweetNaclFast.Signature.KeyPair keyPair =
    // TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    // generate private key and get wallet address, that you top up later
    // TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 randomDestinationWallet =
        WalletV4R2.builder().keyPair(Utils.generateSignatureKeyPair()).walletId(42).build();

    // use your wallet
    WalletV4R2 myWallet =
        WalletV4R2.builder().tonProvider(adnlLiteClient).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress = myWallet.getAddress().toNonBounceable();
    String bounceableAddress = myWallet.getAddress().toBounceable();
    String rawAddress = myWallet.getAddress().toRaw();

    log.info("non-bounceable address: {}", nonBounceableAddress);
    log.info("    bounceable address: {}", bounceableAddress);
    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(myWallet.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(myWallet.getKeyPair().getSecretKey()));

    BigInteger balance = adnlLiteClient.getBalance(Address.of(bounceableAddress));
    log.info("account balance {}", Utils.formatNanoValue(balance));

    //        myWallet.deploy();
    //        myWallet.waitForDeployment(90);

    // get usdt jetton master (minter) address
    JettonMinterStableCoin usdtMasterWallet =
        JettonMinterStableCoin.builder()
            .tonProvider(adnlLiteClient)
            .customAddress(usdtMasterAddress)
            .build();

    log.info(
        "usdt total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

    // get my JettonWallet the one that holds my jettons (USDT) tokens
    JettonWalletStableCoin myJettonWallet = usdtMasterWallet.getJettonWallet(myWallet.getAddress());
    log.info(
        "my jettonWallet balance: {}", Utils.formatJettonValue(myJettonWallet.getBalance(), 6, 2));

    // send my jettons to external address
    WalletV4R2Config walletV4Config =
        WalletV4R2Config.builder()
            .walletId(42)
            .seqno(myWallet.getSeqno())
            .destination(myJettonWallet.getAddress())
            .amount(Utils.toNano(0.02))
            .body(
                JettonWalletStableCoin.createTransferBody(
                    0,
                    BigInteger.valueOf(20000), // 2 cents
                    randomDestinationWallet.getAddress(), // recipient
                    null, // response address
                    BigInteger.ZERO, // forward amount
                    null) // forward payload
                )
            .build();
    SendResponse sendResponse = myWallet.send(walletV4Config);
    assertThat(sendResponse.getCode()).isZero();

    Utils.sleep(
        12, "transferring 0.02 USDT jettons to wallet " + randomDestinationWallet.getAddress());

    BigInteger balanceOfDestinationWallet =
        adnlLiteClient.getBalance(randomDestinationWallet.getAddress());
    log.info("balanceOfDestinationWallet in toncoins: {}", balanceOfDestinationWallet);

    JettonWalletStableCoin randomJettonWallet =
        usdtMasterWallet.getJettonWallet(randomDestinationWallet.getAddress());
    BigInteger randomJettonWalletBalance = randomJettonWallet.getBalance();
    log.info(
        "randomJettonWallet balance in jettons: {}",
        Utils.formatJettonValue(randomJettonWalletBalance, 6, 2));

    assertThat(balanceOfDestinationWallet).isEqualTo(BigInteger.ZERO);
    assertThat(randomJettonWalletBalance).isEqualTo(BigInteger.valueOf(20000));
  }
}
