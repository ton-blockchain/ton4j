package org.ton.ton4j.smartcontract;

import org.ton.ton4j.provider.SendResponse;

import static java.util.Objects.isNull;

import lombok.Builder;
import lombok.Data;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;

import java.math.BigInteger;

@Builder
@Data
public class LibraryDeployer implements Contract {

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private TonProvider tonProvider;

  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private Tonlib tonlib;
  private long wc;
  Cell libraryDeployerCode;
  Cell libraryCode;
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
  public void setTonCenterClient(org.ton.ton4j.toncenter.TonCenter pTonCenterClient) {
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
    return -1;
  }

  @Override
  public String getName() {
    return "LibraryDeployer";
  }

  @Override
  public Cell createCodeCell() {
    if (isNull(libraryDeployerCode)) {
      return CellBuilder.beginCell().fromBoc(WalletCodes.libraryDeployer.getValue()).endCell();
    } else {
      return libraryDeployerCode;
    }
  }

  @Override
  public Cell createDataCell() {
    return libraryCode;
  }

  public static class LibraryDeployerBuilder {}

  public static LibraryDeployerBuilder builder() {
    return new CustomLibraryDeployerBuilder();
  }

  private static class CustomLibraryDeployerBuilder extends LibraryDeployerBuilder {
    @Override
    public LibraryDeployer build() {

      LibraryDeployer instance = super.build();
      if (super.tonProvider != null) {
        instance.setTonProvider(super.tonProvider);
      }
      return instance;
    }
  }

  public Message prepareDeployMsg() {

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .build();
  }

  public SendResponse deploy() {
      return send(prepareDeployMsg());
  }
}
