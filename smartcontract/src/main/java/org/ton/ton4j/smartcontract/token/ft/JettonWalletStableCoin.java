package org.ton.ton4j.smartcontract.token.ft;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.JettonWalletData;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.VmCellSlice;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.model.RunGetMethodResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryCell;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.tonlib.types.TvmStackEntrySlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class JettonWalletStableCoin implements Contract {

  Address address;

  public static Cell CODE_CELL =
      CellBuilder.beginCell().fromBoc(WalletCodes.jettonWalletStableCoin.getValue()).endCell();

  public static Cell CODE_LIB_CELL =
          CellBuilder.beginCell()
                  .fromBoc(
                          "B5EE9C72010101010023000842028F452D7A4DFD74066B682365177259ED05734435BE76B5FD4BD5D8AF2B7C3D68")
                  .endCell();

  public static class JettonWalletStableCoinBuilder {}

  public static JettonWalletStableCoinBuilder builder() {
    return new CustomJettonWalletStableCoinBuilder();
  }

  private static class CustomJettonWalletStableCoinBuilder extends JettonWalletStableCoinBuilder {
    @Override
    public JettonWalletStableCoin build() {
      JettonWalletStableCoin instance = super.build();
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
    return "jettonWalletStableCoin";
  }

  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.jettonWalletStableCoin.getValue()).endCell();
  }

  /**
   * @return Cell cell contains nft data
   */
  public static Cell createTransferBody(
      long queryId,
      BigInteger jettonAmount,
      Address toAddress,
      Address responseAddress,
      BigInteger forwardAmount,
      Cell forwardPayload) {
    return CellBuilder.beginCell()
        .storeUint(0xf8a7ea5, 32)
        .storeUint(queryId, 64) // default
        .storeCoins(jettonAmount)
        .storeAddress(toAddress)
        .storeAddress(responseAddress)
        .storeBit(false) // no custom_payload
        .storeCoins(forwardAmount)
        .storeRefMaybe(forwardPayload)
        .endCell();
  }

  /**
   * @param queryId long
   * @param jettonAmount BigInteger
   * @param responseAddress Address
   */
  public static Cell createBurnBody(
      long queryId, BigInteger jettonAmount, Address responseAddress) {
    return CellBuilder.beginCell()
        .storeUint(0x595f07bc, 32) // burn up
        .storeUint(queryId, 64)
        .storeCoins(jettonAmount)
        .storeAddress(responseAddress)
        .endCell();
  }

  /**
   * Note, status==0 means unlocked - user can freely transfer and receive jettons (only admin can
   * burn). (status and 1) bit means user can not send jettons (status and 2) bit means user can not
   * receive jettons. Master (minter) smart-contract able to make outgoing actions (transfer, burn
   * jettons) with any status.
   */
  public static Cell createStatusBody(long queryId, int status) {
    return CellBuilder.beginCell()
        .storeUint(new BigInteger("eed236d3", 16), 32)
        .storeUint(queryId, 64)
        .storeUint(status, 4)
        .endCell();
  }

  public static Cell createCallToBody(
      long queryId, Address destination, BigInteger tonAmount, Cell payload) {
    return CellBuilder.beginCell()
        .storeUint(0x235caf52, 32)
        .storeUint(queryId, 64)
        .storeAddress(destination)
        .storeCoins(tonAmount)
        .storeRef(payload)
        .endCell();
  }

  public JettonWalletData getData() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        // Use TonCenter API to get jetton wallet data
        RunGetMethodResponse response =
            ((TonCenter) provider)
                .runGetMethod(getAddress().toBounceable(), "get_wallet_data", new ArrayList<>())
                .getResult();

        // Parse the response
        BigInteger balance =
            new BigInteger(
                ((String) new ArrayList<>(response.getStack().get(0)).get(1)).substring(2), 16);

        // Parse owner address from stack
        String ownerAddrHex = ((String) new ArrayList<>(response.getStack().get(1)).get(1));
        Address ownerAddress = Address.of(ownerAddrHex);

        // Parse jetton minter address from stack
        String minterAddrHex = ((String) new ArrayList<>(response.getStack().get(2)).get(1));
        Address jettonMinterAddress = Address.of(minterAddrHex);

        // Parse jetton wallet code from stack
        String codeHex = ((String) new ArrayList<>(response.getStack().get(3)).get(1));
        Cell jettonWalletCode =
            CellBuilder.beginCell().fromBoc(Utils.base64ToBytes(codeHex)).endCell();

        return JettonWalletData.builder()
            .balance(balance)
            .ownerAddress(ownerAddress)
            .jettonMinterAddress(jettonMinterAddress)
            .jettonWalletCode(jettonWalletCode)
            .build();
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet data: " + e.getMessage());
      }
    } else if (provider instanceof AdnlLiteClient) {
      RunMethodResult runMethodResult =
          ((AdnlLiteClient) provider).runMethod(getAddress(), "get_wallet_data");
      BigInteger balance = runMethodResult.getIntByIndex(0);
      VmCellSlice slice = runMethodResult.getSliceByIndex(1);
      Address ownerAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();
      slice = runMethodResult.getSliceByIndex(2);
      Address jettonMinterAddress =
          CellSlice.beginParse(slice.getCell()).skipBits(slice.getStBits()).loadAddress();

      Cell jettonWalletCode = runMethodResult.getCellByIndex(3);
      return JettonWalletData.builder()
          .balance(balance)
          .ownerAddress(ownerAddress)
          .jettonMinterAddress(jettonMinterAddress)
          .jettonWalletCode(jettonWalletCode)
          .build();
    } else if (provider instanceof Tonlib) {
      RunResult result = ((Tonlib) provider).runMethod(address, "get_wallet_data");

      if (result.getExit_code() != 0) {
        throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
      }

      TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
      BigInteger balance = balanceNumber.getNumber();

      TvmStackEntrySlice ownerAddr = (TvmStackEntrySlice) result.getStack().get(1);
      Address ownerAddress =
          NftUtils.parseAddress(
              CellBuilder.beginCell()
                  .fromBoc(Utils.base64ToBytes(ownerAddr.getSlice().getBytes()))
                  .endCell());

      TvmStackEntrySlice jettonMinterAddr = (TvmStackEntrySlice) result.getStack().get(2);
      Address jettonMinterAddress =
          NftUtils.parseAddress(
              CellBuilder.beginCell()
                  .fromBoc(Utils.base64ToBytes(jettonMinterAddr.getSlice().getBytes()))
                  .endCell());

      TvmStackEntryCell jettonWallet = (TvmStackEntryCell) result.getStack().get(3);
      Cell jettonWalletCode =
          CellBuilder.beginCell()
              .fromBoc(Utils.base64ToBytes(jettonWallet.getCell().getBytes()))
              .endCell();
      return JettonWalletData.builder()
          .balance(balance)
          .ownerAddress(ownerAddress)
          .jettonMinterAddress(jettonMinterAddress)
          .jettonWalletCode(jettonWalletCode)
          .build();
    } else {
      throw new Error("Provider not set");
    }
  }

  public BigInteger getBalance() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      try {
        // Use TonCenter API to get jetton wallet balance
        RunGetMethodResponse response =
            ((TonCenter) provider)
                .runGetMethod(getAddress().toBounceable(), "get_wallet_data", new ArrayList<>())
                .getResult();

        // Parse the balance from the response
        String balanceHex = ((String) new java.util.ArrayList<>(response.getStack().get(0)).get(1));
        return new BigInteger(balanceHex.substring(2), 16);
      } catch (Exception e) {
        throw new Error("Error getting jetton wallet balance: " + e.getMessage());
      }
    } else if (provider instanceof AdnlLiteClient) {
      RunMethodResult runMethodResult =
          ((AdnlLiteClient) provider).runMethod(getAddress(), "get_wallet_data");
      return runMethodResult.getIntByIndex(0);
    }

    if (!(provider instanceof Tonlib)) {
      throw new Error("Provider not set");
    }
    RunResult result = ((Tonlib) provider).runMethod(address, "get_wallet_data");

    if (result.getExit_code() != 0) {
      throw new Error("method get_wallet_data, returned an exit code " + result.getExit_code());
    }

    TvmStackEntryNumber balanceNumber = (TvmStackEntryNumber) result.getStack().get(0);
    return balanceNumber.getNumber();
  }
}
