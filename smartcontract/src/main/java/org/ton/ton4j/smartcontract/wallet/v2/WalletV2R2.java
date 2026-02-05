package org.ton.ton4j.smartcontract.wallet.v2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.provider.SendResponse;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.types.WalletV2R2Config;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class WalletV2R2 implements Contract {

  public TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  byte[] publicKey;

  public static class WalletV2R2Builder {}

  public static WalletV2R2Builder builder() {
    return new CustomWalletV2R2Builder();
  }

  private static class CustomWalletV2R2Builder extends WalletV2R2Builder {
    @Override
    public WalletV2R2 build() {
      if (isNull(super.publicKey)) {
        if (isNull(super.keyPair)) {
          super.keyPair = Utils.generateSignatureKeyPair();
        }
      }
      WalletV2R2 instance = super.build();
      if (super.tonProvider != null) {
        instance.setTonProvider(super.tonProvider);
      }
      return instance;
    }
  }

  @Getter(AccessLevel.NONE)
  private TonProvider tonProvider;

  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private Tonlib tonlib;
  private long wc;
  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private AdnlLiteClient adnlLiteClient;
  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private TonCenter tonCenterClient;

  /**
   * used only with TopUp faucets and emulators
   */
  BigInteger initialBalance;

  @Override
  public BigInteger getInitialBalance() {
    return initialBalance;
  }

  @Override
  public AdnlLiteClient getAdnlLiteClient() {
    return adnlLiteClient;
  }

  @Override
  public void setAdnlLiteClient(AdnlLiteClient pAdnlLiteClient) {
    adnlLiteClient = pAdnlLiteClient;
  }

  @Override
  public TonCenter getTonCenterClient() {
    return tonCenterClient;
  }

  @Override
  public void setTonCenterClient(TonCenter pTonCenterClient) {
    tonCenterClient = pTonCenterClient;
  }

  @Override
  public Tonlib getTonlib() {
    return tonlib;
  }

  @Override
  public void setTonlib(Tonlib pTonlib) {
    tonlib = pTonlib;
  }

  @Override
  public long getWorkchain() {
    return wc;
  }

  @Override
  public String getName() {
    return "V2R2";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.V2R2.getValue()).endCell();
  }

  public String getPublicKey() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        return Utils.bytesToHex(
            Utils.to32ByteArray(
                ((TonCenter) provider).getPublicKey(getAddress().toBounceable())));
      } catch (Exception e) {
        throw new Error(e);
      }
    }
    if (provider instanceof AdnlLiteClient) {
      return Utils.bytesToHex(
          Utils.to32ByteArray(((AdnlLiteClient) provider).getPublicKey(getAddress())));
    }
    if (provider instanceof Tonlib) {
      Address myAddress = this.getAddress();
      RunResult result = ((Tonlib) provider).runMethod(myAddress, "get_public_key");

      if (result.getExit_code() != 0) {
        throw new Error("method get_public_key, returned an exit code " + result.getExit_code());
      }

      TvmStackEntryNumber publicKeyNumber = (TvmStackEntryNumber) result.getStack().get(0);
      return publicKeyNumber.getNumber().toString(16);
    }
    throw new Error("Provider not set");
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeUint(initialSeqno, 32)
        .storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey())
        .endCell();
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell().storeUint(initialSeqno, 32).storeInt(-1, 32).endCell();
  }

  /** Creates message payload with seqno and validUntil fields */
  public Cell createTransferBody(WalletV2R2Config config) {

    CellBuilder message = CellBuilder.beginCell();
    message.storeUint(BigInteger.valueOf(config.getSeqno()), 32);

    message.storeUint(
        (config.getValidUntil() == 0)
            ? Instant.now().getEpochSecond() + 60
            : config.getValidUntil(),
        32);

    if (nonNull(config.getDestination1())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination1(),
              config.getAmount1(),
              config.getExtraCurrencies1(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }
    if (nonNull(config.getDestination2())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination2(),
              config.getAmount2(),
              config.getExtraCurrencies2(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }
    if (nonNull(config.getDestination3())) {
      MessageRelaxed order =
          MsgUtils.createInternalMessageRelaxed(
              config.getDestination3(),
              config.getAmount3(),
              config.getExtraCurrencies3(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }
    if (nonNull(config.getDestination4())) {
      Message order =
          MsgUtils.createInternalMessage(
              getAddress(),
              config.getDestination4(),
              config.getAmount3(),
              config.getExtraCurrencies4(),
              config.getStateInit(),
              config.getBody(),
              config.getBounce());
      message.storeUint(
          isNull(config.getSendMode()) // for backward compatibility
              ? ((config.getMode() == 0) ? 3 : config.getMode())
              : config.getSendMode().getValue(),
          8);
      message.storeRef(order.toCell());
    }

    return message.endCell();
  }

  public SendResponse send(WalletV2R2Config config) {
    return send(prepareExternalMsg(config));
  }

  /**
   * Sends amount of nano toncoins to the destination address and waits till a message found among
   * account's transactions
   */
  public Transaction sendWithConfirmation(WalletV2R2Config config) throws Exception {
    TonProvider provider = getTonProvider();
    return provider.sendExternalMessageWithConfirmation(prepareExternalMsg(config));
  }

  public Message prepareExternalMsg(WalletV2R2Config config) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(keyPair, getAddress(), null, body);
  }

  public SendResponse deploy() {
    return send(prepareDeployMsg());
  }

  public SendResponse deploy(byte[] signedBody) {
    return send(prepareDeployMsg(signedBody));
  }

  public Message prepareDeployMsg(byte[] signedBodyHash) {
    Cell body = createDeployMessage();
    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeBytes(signedBodyHash).storeCell(body).endCell())
        .build();
  }

  public SendResponse send(WalletV2R2Config config, byte[] signedBodyHash) {
    return send(prepareExternalMsg(config, signedBodyHash));
  }

  public Message prepareExternalMsg(WalletV2R2Config config, byte[] signedBodyHash) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(signedBodyHash, getAddress(), null, body);
  }

  public Message prepareDeployMsg() {
    Cell body = createDeployMessage();

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(
                    Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                .storeCell(body)
                .endCell())
        .build();
  }

  public Cell createInternalSignedBody(WalletV2R2Config config) {
    Cell body = createTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Message prepareInternalMsg(WalletV2R2Config config) {
    Cell body = createInternalSignedBody(config);

    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .srcAddr(getAddressIntStd())
                .dstAddr(getAddressIntStd())
                .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                .build())
        .body(body)
        .build();
  }

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV2R2Config config) {
    Cell body = createInternalSignedBody(config);

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .dstAddr(getAddressIntStd())
                .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                .build())
        .body(body)
        .build();
  }
}
