package org.ton.ton4j.smartcontract;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Data;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

import java.math.BigInteger;

@Builder
@Data
public class GenericSmartContract implements Contract {

  TweetNaclFast.Signature.KeyPair keyPair;
  byte[] publicKey;

  String code;
  String data;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
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
  public org.ton.ton4j.toncenter.TonCenter getTonCenterClient() {
    return tonCenterClient;
  }

  @Override
  public void setTonCenterClient(org.ton.ton4j.toncenter.TonCenter pTonCenterClient) {
    tonCenterClient = pTonCenterClient;
  }

  @Override
  public long getWorkchain() {
    return wc;
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
  public String getName() {
    return "GenericSmartContract";
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(code).endCell();
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().fromBoc(data).endCell();
  }

  public static class GenericSmartContractBuilder {}

  public static GenericSmartContractBuilder builder() {
    return new CustomGenericSmartContractBuilder();
  }

  private static class CustomGenericSmartContractBuilder extends GenericSmartContractBuilder {
    @Override
    public GenericSmartContract build() {
      GenericSmartContract instance = super.build();
      if (super.tonProvider != null) {
        instance.setTonProvider(super.tonProvider);
      }
      return instance;
    }
  }

  /**
   * Deploy with body
   *
   * @param deployMessageBody usually stands for internal message
   * @return SendResponse
   */
  public SendResponse deploy(Cell deployMessageBody) {
    return send(prepareDeployMsg(deployMessageBody));
  }

  /**
   * Deploy with body without signing it.
   *
   * @param deployMessageBody usually stands for internal message
   * @return SendResponse
   */
  public SendResponse deployWithoutSignature(Cell deployMessageBody) {
    return send(prepareDeployMsgWithoutSignature(deployMessageBody));
  }

  /**
   * Deploy without body
   *
   * @return SendResponse
   */
  public SendResponse deploy() {
    return send(prepareDeployMsgWithoutBody());
  }

  public SendResponse deploy(Cell deployMessageBody, byte[] signedBody) {
    return send(prepareDeployMsg(deployMessageBody, signedBody));
  }

  public Message prepareDeployMsgWithoutBody() {
    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .build();
  }

  public Message prepareDeployMsg(Cell deployMessageBody, byte[] signedBodyHash) {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(signedBodyHash)
                .storeCell(deployMessageBody)
                .endCell())
        .build();
  }

  public Message prepareDeployMsg(Cell deployMessageBody) {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(
                    Utils.signData(
                        keyPair.getPublicKey(), keyPair.getSecretKey(), deployMessageBody.hash()))
                .storeCell(deployMessageBody)
                .endCell())
        .build();
  }

  public Message prepareDeployMsgWithoutSignature(Cell deployMessageBody) {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(deployMessageBody)
        .build();
  }

  public Cell createDeployMessage() {
    return createDataCell();
  }

  @Override
  public Message prepareDeployMsg() {
    Cell body = createDeployMessage();

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(
            CellBuilder.beginCell()
                .storeBytes(
                    isNull(keyPair)
                        ? new byte[0]
                        : Utils.signData(
                            keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                .storeCell(body)
                .endCell())
        .build();
  }
}
