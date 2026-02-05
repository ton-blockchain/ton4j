package org.ton.ton4j.smartcontract.token.ft;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonMinterData;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.VmCellSlice;
import org.ton.ton4j.tlb.VmStackValueSlice;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryCell;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
@Slf4j
public class JettonMinterV2 implements Contract {
  Address adminAddress;
  Address nextAdminAddress;
  Cell content;
  Address customAddress;

  public static Cell CODE_CELL =
      CellBuilder.beginCell().fromBoc(WalletCodes.jettonMinterV2.getValue()).endCell();

  public static class JettonMinterV2Builder {}

  public static JettonMinterV2Builder builder() {
    return new CustomJettonMinterV2Builder();
  }

  private static class CustomJettonMinterV2Builder extends JettonMinterV2Builder {
    @Override
    public JettonMinterV2 build() {
      JettonMinterV2 instance = super.build();
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

  public String getName() {
    return "jettonMinter";
  }

  /**
   * storage#_ total_supply:Coins admin_address:MsgAddress next_admin_address:MsgAddress
   * jetton_wallet_code:^Cell metadata_uri:^Cell = Storage;
   *
   * @return Cell - contains jetton data cell
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell()
        .storeCoins(BigInteger.ZERO)
        .storeAddress(adminAddress)
        .storeAddress(nextAdminAddress)
        .storeRef(CellBuilder.beginCell().fromBoc(WalletCodes.jettonWalletV2.getValue()).endCell())
        .storeRef(content)
        .endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.jettonMinterV2.getValue()).endCell();
  }

  /**
   * @param queryId long
   * @param destination Address
   * @param amount BigInteger
   * @param jettonAmount BigInteger
   * @param fromAddress Address
   * @param responseAddress Address
   * @param forwardTonAmount BigInteger
   * @param forwardPayload Cell
   * @return Cell
   */
  public static Cell createMintBody(
      long queryId,
      Address destination,
      BigInteger amount,
      BigInteger jettonAmount,
      Address fromAddress,
      Address responseAddress,
      BigInteger forwardTonAmount,
      Cell forwardPayload) {
    return CellBuilder.beginCell()
        .storeUint(0x642b7d07, 32) // OP mint
        .storeUint(queryId, 64) // query_id, default 0
        .storeAddress(destination)
        .storeCoins(amount)
        .storeRef(
            CellBuilder.beginCell()
                .storeUint(0x178d4519, 32) // internal_transfer op
                .storeUint(queryId, 64) // default 0
                .storeCoins(jettonAmount)
                .storeAddress(fromAddress) // from_address
                .storeAddress(responseAddress) // response_address
                .storeCoins(forwardTonAmount) // forward_ton_amount
                //                .storeBit(true) // store payload in the same cell
                .storeRefMaybe(forwardPayload) // forward payload
                .endCell())
        .endCell();
  }

  /**
   * @return JettonData
   */
  public JettonMinterData getJettonData() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {

      org.ton.ton4j.toncenter.model.JettonMinterData data;
      if (nonNull(customAddress)) {
        data = ((TonCenter) provider).getJettonData(customAddress.toBounceable());
      } else {
        data = ((TonCenter) provider).getJettonData(getAddress().toBounceable());
      }
      return JettonMinterData.builder()
          .adminAddress(data.getAdminAddress())
          .isMutable(data.isMutable())
          .jettonContentCell(data.getJettonContentCell())
          .jettonContentUri(data.getJettonContentUri())
          .jettonWalletCode(data.getJettonWalletCode())
          .totalSupply(data.getTotalSupply())
          .build();

    } else if (provider instanceof AdnlLiteClient) {
      RunMethodResult runMethodResult;
      if (nonNull(customAddress)) {
        runMethodResult =
            ((AdnlLiteClient) provider).runMethod(customAddress, "get_jetton_data");
      } else {
        runMethodResult = ((AdnlLiteClient) provider).runMethod(getAddress(), "get_jetton_data");
      }

      BigInteger totalSupply = runMethodResult.getIntByIndex(0);
      boolean isMutable = runMethodResult.getIntByIndex(1).intValue() == -1;
      VmCellSlice slice = runMethodResult.getSliceByIndex(2);
      Address adminAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();

      Cell jettonContentCell = runMethodResult.getCellByIndex(3);
      String jettonContentUri = null;
      try {
        jettonContentUri = NftUtils.parseOffChainUriCell(jettonContentCell);
      } catch (Error e) {
        // todo
      }

      Cell jettonWalletCode = runMethodResult.getCellByIndex(4);

      return JettonMinterData.builder()
          .totalSupply(totalSupply)
          .isMutable(isMutable)
          .adminAddress(adminAddress)
          .jettonContentCell(jettonContentCell)
          .jettonContentUri(jettonContentUri)
          .jettonWalletCode(jettonWalletCode)
          .build();
    } else if (provider instanceof Tonlib) {
      RunResult result;
      if (nonNull(customAddress)) {
        result = ((Tonlib) provider).runMethod(customAddress, "get_jetton_data");
      } else {
        result = ((Tonlib) provider).runMethod(getAddress(), "get_jetton_data");
      }

    if (result.getExit_code() != 0) {
      throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
    BigInteger totalSupply = totalSupplyNumber.getNumber();

    boolean isMutable =
        ((TvmStackEntryNumber) result.getStack().get(1)).getNumber().longValue() == -1;

    TvmStackEntrySlice adminAddr = (TvmStackEntrySlice) result.getStack().get(2);
    Address adminAddress =
        NftUtils.parseAddress(
            CellBuilder.beginCell()
                .fromBoc(Utils.base64ToBytes(adminAddr.getSlice().getBytes()))
                .endCell());

    TvmStackEntryCell jettonContent = (TvmStackEntryCell) result.getStack().get(3);
    Cell jettonContentCell =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(jettonContent.getCell().getBytes()))
            .endCell();
    String jettonContentUri = null;
    try {
      jettonContentUri = NftUtils.parseOffChainUriCell(jettonContentCell);
    } catch (Error e) {
      // todo
    }

    TvmStackEntryCell contentC = (TvmStackEntryCell) result.getStack().get(4);
    Cell jettonWalletCode =
        CellBuilder.beginCell()
            .fromBoc(Utils.base64ToBytes(contentC.getCell().getBytes()))
            .endCell();

      return JettonMinterData.builder()
        .totalSupply(totalSupply)
        .isMutable(isMutable)
        .adminAddress(adminAddress)
        .jettonContentCell(jettonContentCell)
        .jettonContentUri(jettonContentUri)
        .jettonWalletCode(jettonWalletCode)
        .build();
    } else {
      throw new Error("Provider not set");
    }
  }

  public BigInteger getTotalSupply() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        if (nonNull(customAddress)) {
          return ((TonCenter) provider)
              .getJettonData(customAddress.toBounceable())
              .getTotalSupply();
        } else {
          return ((TonCenter) provider).getJettonData(getAddress().toBounceable()).getTotalSupply();
        }
      } catch (Exception e) {
        throw new Error("Error getting total supply: " + e.getMessage());
      }
    } else if (provider instanceof AdnlLiteClient) {
      RunMethodResult runMethodResult;
      if (nonNull(customAddress)) {
        runMethodResult =
            ((AdnlLiteClient) provider).runMethod(customAddress, "get_jetton_data");
      } else {
        runMethodResult = ((AdnlLiteClient) provider).runMethod(getAddress(), "get_jetton_data");
      }
      return runMethodResult.getIntByIndex(0);
    }

    if (provider instanceof Tonlib) {
      RunResult result;
      if (nonNull(customAddress)) {
        result = ((Tonlib) provider).runMethod(customAddress, "get_jetton_data");
      } else {
        result = ((Tonlib) provider).runMethod(getAddress(), "get_jetton_data");
      }
      if (result.getExit_code() != 0) {
        throw new Error("method get_jetton_data, returned an exit code " + result.getExit_code());
      }

      TvmStackEntryNumber totalSupplyNumber = (TvmStackEntryNumber) result.getStack().get(0);
      return totalSupplyNumber.getNumber();
    }
    throw new Error("Provider not set");
  }

  /**
   * @param queryId long
   * @param newAdminAddress Address
   * @return Cell
   */
  public static Cell createChangeAdminBody(long queryId, Address newAdminAddress) {
    if (isNull(newAdminAddress)) {
      throw new Error("Specify newAdminAddress");
    }

    return CellBuilder.beginCell()
        .storeUint(0x6501f354L, 32)
        .storeUint(queryId, 64)
        .storeAddress(newAdminAddress)
        .endCell();
  }

  public static Cell createUpgradeBody(long queryId, Cell data, Cell code) {
    return CellBuilder.beginCell()
        .storeUint(0x2508d66aL, 32)
        .storeUint(queryId, 64)
        .storeRef(data)
        .storeRef(code)
        .endCell();
  }

  /**
   * @param jettonContentUri String
   * @param queryId long
   * @return Cell
   */
  public static Cell createChangeMetaDataUriBody(String jettonContentUri, long queryId) {
    return CellBuilder.beginCell()
        .storeUint(0xcb862902L, 32)
        .storeUint(queryId, 64)
        .storeRef(NftUtils.createOffChainUriCell(jettonContentUri))
        .endCell();
  }

  public static Cell createClaimAdminBody(long queryId) {
    return CellBuilder.beginCell().storeUint(0xfb88e119L, 32).storeUint(queryId, 64).endCell();
  }

  /**
   * @param ownerAddress Address
   * @return Address user_jetton_wallet_address
   */
  public JettonWalletV2 getJettonWallet(Address ownerAddress) {
    Cell cellAddr = CellBuilder.beginCell().storeAddress(ownerAddress).endCell();
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        Address jettonWalletAddress;
        if (nonNull(customAddress)) {
          jettonWalletAddress =
              ((TonCenter) provider)
                  .getJettonWalletAddress(
                  customAddress.toBounceable(), ownerAddress.toBounceable());
        } else {
          jettonWalletAddress =
              ((TonCenter) provider)
                  .getJettonWalletAddress(
                  getAddress().toBounceable(), ownerAddress.toBounceable());
        }

        return JettonWalletV2.builder()
            .tonProvider((TonCenter) provider)
            .address(jettonWalletAddress)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet address: " + e.getMessage());
      }
    } else if (provider instanceof AdnlLiteClient) {
      RunMethodResult runMethodResult;
      if (nonNull(customAddress)) {
        runMethodResult =
            ((AdnlLiteClient) provider)
                .runMethod(
                customAddress,
                "get_wallet_address",
                //                VmStackValueCell.builder().cell(cellAddr).build());
                VmStackValueSlice.builder()
                    .cell(VmCellSlice.builder().cell(cellAddr).build())
                    .build());
      } else {
        runMethodResult =
            ((AdnlLiteClient) provider)
                .runMethod(
                getAddress(),
                "get_wallet_address",
                //                VmStackValueCell.builder().cell(cellAddr).build());
                VmStackValueSlice.builder()
                    .cell(VmCellSlice.builder().cell(cellAddr).build())
                    .build());
      }

      if (runMethodResult.getExitCode() != 0) {
        throw new Error(
            "method get_wallet_address returned an exit code " + runMethodResult.getExitCode());
      }
      VmCellSlice slice = runMethodResult.getSliceByIndex(0);
      //      System.out.println("adnl sliceBytes " + Utils.bytesToHex(slice.getCell().toBoc()));
      Address jettonWalletAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();

      return JettonWalletV2.builder()
          .tonProvider(provider)
          .address(jettonWalletAddress)
          .build();
    }

    if (provider instanceof Tonlib) {
      Deque<String> stack = new ArrayDeque<>();
      stack.offer("[slice, " + cellAddr.toHex(true) + "]");

      RunResult result;

      if (nonNull(customAddress)) {
        result = ((Tonlib) provider).runMethod(customAddress, "get_wallet_address", stack);
      } else {
        result = ((Tonlib) provider).runMethod(getAddress(), "get_wallet_address", stack);
      }

      if (result.getExit_code() != 0) {
        throw new Error(
            "method get_wallet_address, returned an exit code " + result.getExit_code());
      }

      TvmStackEntrySlice addr = (TvmStackEntrySlice) result.getStack().get(0);
      byte[] sliceBytes = Utils.base64ToBytes(addr.getSlice().getBytes());
      //    System.out.println("tonlib sliceBytes " + Utils.bytesToHex(sliceBytes));
      Address jettonWalletAddress =
          NftUtils.parseAddress(CellBuilder.beginCell().fromBoc(sliceBytes).endCell());

      return JettonWalletV2.builder()
          .tonProvider(provider)
          .address(jettonWalletAddress)
          .build();
    }
    throw new Error("Provider not set");
  }
}
