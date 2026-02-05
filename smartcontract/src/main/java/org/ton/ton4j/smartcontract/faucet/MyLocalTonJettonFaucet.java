package org.ton.ton4j.smartcontract.faucet;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;

import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.ContractUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

/** Faucet for NEOJ jettons. */
@Slf4j
public class MyLocalTonJettonFaucet {

  public static String ADMIN_WALLET_PUBLIC_KEY =
      "d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  static String ADMIN_WALLET_SECRET_KEY =
      "be0bbb1725807ec0df984702a32a143864418400d797a48e267a120c3dc5f8d0d1d4515b2635b81de98d58f65502f2c242bb0e63615520341b83a12dd4d0f516";
  public static String ADMIN_WALLET_ADDRESS =
      "0:98972d1ab4b86f6be34ad03d64bb5e2cb369f0d7b5e53f13348664672b893010";
  public static String ADMIN_WALLET_BOUNCEABLE_ADDRESS =
      "EQCYly0atLhva-NK0D1ku14ss2nw17XlPxM0hmRnK4kwEO86";
  public static String FAUCET_MASTER_ADDRESS = "0:502e0f88ac0c2e0e56bfd51e2b828ebe20e32d8d80d4b8f7e7bd72bfeb9d59b9";

  public static BigInteger topUpContractWithNeoj(
      TonProvider tonProvider, Address destinationAddress, BigInteger jettonsAmount) {
    return topUpContractWithNeoj(tonProvider, destinationAddress, jettonsAmount, false);
  }

  public static BigInteger topUpContractWithNeoj(
      TonProvider tonProvider,
      Address destinationAddress,
      BigInteger jettonsAmount,
      boolean avoidRateLimit) {
    if (tonProvider instanceof TonCenter) {
      return topUpContractWithNeoj(
          (TonCenter) tonProvider, destinationAddress, jettonsAmount, avoidRateLimit);
    }
    if (tonProvider instanceof AdnlLiteClient) {
      return topUpContractWithNeoj((AdnlLiteClient) tonProvider, destinationAddress, jettonsAmount);
    }
    if (tonProvider instanceof Tonlib) {
      return topUpContractWithNeoj((Tonlib) tonProvider, destinationAddress, jettonsAmount);
    }
    throw new Error(
        "Unsupported TonProvider implementation: "
            + (tonProvider == null ? "null" : tonProvider.getClass()));
  }

  /**
   * @deprecated Use {@link #topUpContractWithNeoj(TonProvider, Address, BigInteger)}.
   */
  @Deprecated
  public static BigInteger topUpContractWithNeoj(
      Tonlib tonlib, Address destinationAddress, BigInteger jettonsAmount) {

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonProvider(tonlib).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
            JettonMinter.builder()
                    .adminAddress(adminWallet.getAddress())
                    .content(
                            NftUtils.createOffChainUriCell(
                                    "https://raw.githubusercontent.com/ton-blockchain/ton4j/main/1-media/neo-jetton.json"))
                    .build();

    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    jettonsAmount,
                    destinationAddress, // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody(
                        "jetton top up from ton4j faucet") // forward payload
                    ))
            .build();
    SendResponse sendResponse = adminWallet.send(walletV3Config);

    if (sendResponse.getCode() != 0) {
      throw new Error(sendResponse.getMessage());
    }

    ContractUtils.waitForJettonBalanceChange(
        tonlib, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress, 60);
    Utils.sleep(10);
    return ContractUtils.getJettonBalance(
        tonlib, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
  }

  /**
   * @deprecated Use {@link #topUpContractWithNeoj(TonProvider, Address, BigInteger)}.
   */
  @Deprecated
  public static BigInteger topUpContractWithNeoj(
      AdnlLiteClient adnlLiteClient, Address destinationAddress, BigInteger jettonsAmount) {

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().adnlLiteClient(adnlLiteClient).walletId(42).keyPair(keyPair).build();

    JettonMinter minter =
            JettonMinter.builder()
                    .adnlLiteClient(adnlLiteClient)
                    .adminAddress(adminWallet.getAddress())
                    .content(
                            NftUtils.createOffChainUriCell(
                                    "https://raw.githubusercontent.com/ton-blockchain/ton4j/main/1-media/neo-jetton.json"))
                    .build();

    JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    jettonsAmount,
                    destinationAddress, // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody(
                        "jetton top up from ton4j faucet") // forward payload
                    ))
            .build();
    SendResponse sendResponse = adminWallet.send(walletV3Config);

    if (sendResponse.getCode() != 0) {
      throw new Error(sendResponse.getMessage());
    }

    ContractUtils.waitForJettonBalanceChange(
        adnlLiteClient, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress, 60);

    Utils.sleep(10);
    return ContractUtils.getJettonBalance(
        adnlLiteClient, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
  }

  /**
   * @deprecated Use {@link #topUpContractWithNeoj(TonProvider, Address, BigInteger, boolean)}.
   */
  @Deprecated
  public static BigInteger topUpContractWithNeoj(
      TonCenter tonCenterClient,
      Address destinationAddress,
      BigInteger jettonsAmount,
      boolean avoidRateLimit) {

//    if (jettonsAmount.compareTo(Utils.toNano(100)) > 0) {
//      throw new Error(
//          "Too many NEOJ jettons requested from the TestnetJettonFaucet, maximum amount per request is 100.");
//    }

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonCenterClient(tonCenterClient).walletId(42).keyPair(keyPair).build();

    JettonMinter jettonMinterWallet =
        JettonMinter.builder()
            .tonCenterClient(tonCenterClient)
            .customAddress(Address.of(FAUCET_MASTER_ADDRESS))
            .build();

    JettonWallet adminJettonWallet = jettonMinterWallet.getJettonWallet(adminWallet.getAddress());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    jettonsAmount,
                    destinationAddress, // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody(
                        "jetton top up from ton4j faucet") // forward payload
                    ))
            .build();

    SendResponse sendResponse = adminWallet.send(walletV3Config);

    if (sendResponse.getCode() != 0) {
      throw new Error(sendResponse.getMessage());
    }

    // Wait for jetton balance change
    try {
      BigInteger initialBalance =
          ContractUtils.getJettonBalance(
              tonCenterClient, Address.of(FAUCET_MASTER_ADDRESS),destinationAddress);
      int timeoutSeconds = 60;
      int i = 0;
      BigInteger currentBalance;
      do {
        if (++i * 2 >= timeoutSeconds) {
          throw new Error("Jetton balance was not changed within specified timeout.");
        }
        Utils.sleep(2);
        currentBalance =
            ContractUtils.getJettonBalance(
                tonCenterClient, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
      } while (initialBalance.equals(currentBalance));
    } catch (Exception e) {
      throw new Error("Error waiting for jetton balance change: " + e.getMessage());
    }

    Utils.sleep(10);
    return ContractUtils.getJettonBalance(
        tonCenterClient, Address.of(FAUCET_MASTER_ADDRESS), destinationAddress);
  }

  public static WalletV3R2 deployJettonWalletAdminContract(TonProvider tonProvider)
      throws Exception {
    byte[] secretKey = Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY);
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV3R2 adminWallet =
        WalletV3R2.builder().tonProvider(tonProvider).walletId(42).keyPair(keyPair).build();

    if (!adminWallet.isDeployed()) {
      log.info("admin wallet private key {}", Utils.bytesToHex(keyPair.getSecretKey()));
      log.info("admin wallet public key {}", Utils.bytesToHex(keyPair.getPublicKey()));
      log.info("Non-bounceable address (for init): {}", adminWallet.getAddress().toNonBounceable());
      log.info(
          "Bounceable address (for later access): {}", adminWallet.getAddress().toBounceable());
      log.info("Raw address: {}", adminWallet.getAddress().toRaw());

      MyLocalTonFaucet.topUpContract(
          tonProvider, Address.of(adminWallet.getAddress().toNonBounceable()), Utils.toNano(10));

      adminWallet.deploy();
      adminWallet.waitForDeployment();
    }

    return adminWallet;
  }

  /**
   * @deprecated Use {@link #deployJettonWalletAdminContract(TonProvider)}.
   */
  @Deprecated
  public static WalletV3R2 deployJettonWalletAdminContract(AdnlLiteClient adnlLiteClient)
      throws Exception {
    return deployJettonWalletAdminContract((TonProvider) adnlLiteClient);
  }

  public static JettonMinter deployJettonFaucetMinter(
      TonProvider tonProvider, WalletV3R2 adminWallet) {
    log.info("adminWallet {}", adminWallet.getAddress().toRaw());

    JettonMinter minter =
        JettonMinter.builder()
            .tonProvider(tonProvider)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/ton-blockchain/ton4j/main/1-media/neo-jetton.json"))
            .build();

    log.info("jetton minter address {}", minter.getAddress().toRaw());

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(2))
            .stateInit(minter.getStateInit())
            .comment("deploy minter")
            .build();

    adminWallet.send(walletV3Config);
    minter.waitForDeployment();
    log.info(
        "jetton minter deployed, balance {} toncoins", Utils.formatNanoValue(minter.getBalance()));
    return minter;
  }

  /**
   * @deprecated Use {@link #deployJettonFaucetMinter(TonProvider, WalletV3R2)}.
   */
  @Deprecated
  public static JettonMinter deployJettonFaucetMinter(
      AdnlLiteClient adnlLiteClient, WalletV3R2 adminWallet) {
    return deployJettonFaucetMinter((TonProvider) adnlLiteClient, adminWallet);
  }

  public static void mintJettons(WalletV3R2 adminWallet) {
    log.info("minting...");
    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(adminWallet.getSeqno())
            .destination(Address.of(FAUCET_MASTER_ADDRESS))
            .amount(Utils.toNano(0.1))
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.1),
                    new BigInteger("10000000000000000"),
                    null,
                    null,
                    BigInteger.ONE,
                    MsgUtils.createTextMessageBody("minting")))
            .build();

    adminWallet.send(walletV3Config);
    adminWallet.waitForBalanceChange();
    log.info("minted 1000000000000000000 jettons");
  }

  public static BigInteger getUserJettonBalance(TonProvider tonProvider, Address userAddress) {
    if (tonProvider instanceof TonCenter) {
      return ContractUtils.getJettonBalance(
          (TonCenter) tonProvider, Address.of(FAUCET_MASTER_ADDRESS), Address.of(userAddress));
    }
    if (tonProvider instanceof AdnlLiteClient) {
      return ContractUtils.getJettonBalance(
          (AdnlLiteClient) tonProvider, Address.of(FAUCET_MASTER_ADDRESS), Address.of(userAddress));
    }
    if (tonProvider instanceof Tonlib) {
      return ContractUtils.getJettonBalance(
          (Tonlib) tonProvider, Address.of(FAUCET_MASTER_ADDRESS), Address.of(userAddress));
    }
    throw new Error(
        "Unsupported TonProvider implementation: "
            + (tonProvider == null ? "null" : tonProvider.getClass()));
  }

  /**
   * @deprecated Use {@link #getUserJettonBalance(TonProvider, Address)}.
   */
  @Deprecated
  public static BigInteger getUserJettonBalance(
      AdnlLiteClient adnlLiteClient, Address userAddress) {
    return getUserJettonBalance((TonProvider) adnlLiteClient, userAddress);
  }

  public static BigInteger getTotalSupply(TonProvider tonProvider) {
    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(Utils.hexToSignedBytes(ADMIN_WALLET_SECRET_KEY));

    WalletV3R2 adminWallet = WalletV3R2.builder().walletId(42).keyPair(keyPair).build();
    JettonMinter minter =
        JettonMinter.builder()
            .tonProvider(tonProvider)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/ton-blockchain/ton4j/main/1-media/neo-jetton.json"))
            .build();
    return minter.getTotalSupply();
  }

  /**
   * @deprecated Use {@link #getTotalSupply(TonProvider)}.
   */
  @Deprecated
  public static BigInteger getTotalSupply(AdnlLiteClient adnlLiteClient) {
    return getTotalSupply((TonProvider) adnlLiteClient);
  }

  public static void deploy(TonProvider tonProvider) throws Exception {
    WalletV3R2 adminWallet = deployJettonWalletAdminContract(tonProvider);
    // deployJettonFaucetMinter
    JettonMinter jettonMinter = deployJettonFaucetMinter(tonProvider, adminWallet);
    log.info("minter address {}", jettonMinter.getAddress().toRaw());

    // mint NEOJ tokens
    mintJettons(adminWallet);
    log.info("totalSupply {}", getTotalSupply(tonProvider));
  }

  /**
   * @deprecated Use {@link #deploy(TonProvider)}.
   */
  @Deprecated
  public static void deploy(AdnlLiteClient adnlLiteClient) throws Exception {
    deploy((TonProvider) adnlLiteClient);
  }
}
