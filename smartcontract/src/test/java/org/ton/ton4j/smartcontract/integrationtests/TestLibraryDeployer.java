package org.ton.ton4j.smartcontract.integrationtests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.LibraryDeployer;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.tl.liteserver.responses.LibraryEntry;
import org.ton.ton4j.tl.liteserver.responses.LibraryResult;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.GetLibrariesResponse;
import org.ton.ton4j.tonlib.types.SmcLibraryEntry;
import org.ton.ton4j.tonlib.types.SmcLibraryResult;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestLibraryDeployer extends CommonTest {

  /**
   * tops up and deploys library deployer (in master workchain -1) that caries in the
   * StateInit.data() v5r1 code
   */
  @Test
  public void testDeployLibraryDeployer() throws InterruptedException {

    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    //    Cell walletV5Code =
    // CellBuilder.beginCell().fromBoc(WalletCodes.V4R2.getValue()).endCell(); // v4r2 hash
    // /rX/aCDi/w2Ug+fg1iyBfYRniftK5YDIeIZtlZ2r1cA=

    LibraryDeployer libraryDeployer =
        LibraryDeployer.builder().tonProvider(tonlib).libraryCode(walletV5Code).build();

    log.info("boc {}", walletV5Code.toHex());

    String nonBounceableAddressLib = libraryDeployer.getAddress().toNonBounceable();
    log.info("nonBounceable addressLib {}", nonBounceableAddressLib);
    log.info("raw address {}", libraryDeployer.getAddress().toRaw());

    BigInteger balanceLib =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddressLib), Utils.toNano(1));
    log.info(
        "new wallet {} balance: {}", libraryDeployer.getName(), Utils.formatNanoValue(balanceLib));
    libraryDeployer.deploy();

    Utils.sleep(
        1,
        "Deployment of LibraryDeployer will never happen. Lite-server will return an error, but the library will be deployed");

    log.info(
        "walletV5Code library hash(b64) in testnet {}",
        Utils.bytesToBase64(walletV5Code.getHash()));

    SmcLibraryResult smcLibraryResult =
        tonlib.getLibraries(
            Collections.singletonList(
                Utils.bytesToBase64(walletV5Code.getHash())
                //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="
                ));
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (SmcLibraryEntry l : smcLibraryResult.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      log.info("cell lib {}", lib.toHex());
    }
  }

  @Test
  public void testIfLibraryHasBeenDeployed() {
    //    Cell walletV5Code =
    // CellBuilder.beginCell().fromBoc(WalletCodes.V4R2.getValue()).endCell();
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    log.info(
        "walletV5Code library hash(hex) in testnet {}", Utils.bytesToHex(walletV5Code.getHash()));
    log.info(
        "walletV5Code library hash(b64) in testnet {}",
        Utils.bytesToBase64(walletV5Code.getHash()));
    SmcLibraryResult smcLibraryResult =
        tonlib.getLibraries(Collections.singletonList(Utils.bytesToBase64(walletV5Code.getHash())));
    //
    // tonlib.getLibraries(Collections.singletonList("IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="));

    log.info("smcLibraryResult {}", smcLibraryResult);
    log.info("hex {}", Utils.bytesToHex(walletV5Code.getHash()));

    for (SmcLibraryEntry l : smcLibraryResult.getResult()) {
      String cellLibBoc = l.getData();
      Cell lib = Cell.fromBocBase64(cellLibBoc);
      log.info("cell lib {}", lib.toHex());
      // x.elements.put(1L, lib);
    }
  }

  @Test
  public void testDeployLibraryDeployerAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(2)
            .build();
    //    Cell walletV5Code =
    // CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V4R2.getValue()).endCell();

    LibraryDeployer libraryDeployer =
        LibraryDeployer.builder().tonProvider(adnlLiteClient).libraryCode(walletV5Code).build();

    log.info("boc {}", walletV5Code.toHex());

    String nonBounceableAddressLib = libraryDeployer.getAddress().toNonBounceable();
    log.info("nonBounceable addressLib {}", nonBounceableAddressLib);
    log.info("raw address {}", libraryDeployer.getAddress().toRaw());

    BigInteger balanceLib =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddressLib), Utils.toNano(1));
    log.info(
        "new wallet {} balance: {}", libraryDeployer.getName(), Utils.formatNanoValue(balanceLib));
    libraryDeployer.deploy();

    Utils.sleep(
        1,
        "Deployment of LibraryDeployer will never happen. Lite-server will return an error, but the library will be deployed");

    LibraryResult smcLibraryResult =
        adnlLiteClient.getLibraries(
            Collections.singletonList(
                Utils.base64ToBytes(Utils.bytesToBase64(walletV5Code.getHash()))
                //                "IINLe3KxEhR+Gy+0V7hOdNGjDwT3N9T2KmaOlVLSty8="
                //
                // Utils.base64ToBytes("/rX/aCDi/w2Ug+fg1iyBfYRniftK5YDIeIZtlZ2r1cA=")
                ));
    //
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (LibraryEntry lib : smcLibraryResult.getResult()) {
      Cell libCell = Cell.fromBoc(lib.data);
      log.info("cell lib {}", libCell.toHex());
    }
  }

  @Test
  public void testDeployLibraryDeployerTonCenterClient() throws Exception {
    TonCenter tonCenter = TonCenter.builder().apiKey(TESTNET_API_KEY).testnet().debug().build();
    //    Cell walletV5Code =
    // CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V4R2.getValue()).endCell();

    LibraryDeployer libraryDeployer =
        LibraryDeployer.builder().tonProvider(tonCenter).libraryCode(walletV5Code).build();

    log.info("boc {}", walletV5Code.toHex());

    String nonBounceableAddressLib = libraryDeployer.getAddress().toNonBounceable();
    log.info("nonBounceable addressLib {}", nonBounceableAddressLib);
    log.info("raw address {}", libraryDeployer.getAddress().toRaw());

    BigInteger balanceLib =
        TestnetFaucet.topUpContract(
            tonCenter, Address.of(nonBounceableAddressLib), Utils.toNano(1));
    log.info(
        "new wallet {} balance: {}", libraryDeployer.getName(), Utils.formatNanoValue(balanceLib));
    libraryDeployer.deploy();

    Utils.sleep(
        1,
        "Deployment of LibraryDeployer will never happen. Lite-server will return an error, but the library will be deployed");

    TonResponse<GetLibrariesResponse> smcLibraryResult =
        tonCenter.getLibraries(
            Collections.singletonList(Utils.bytesToBase64(walletV5Code.getHash())));
    log.info("response {}", smcLibraryResult.getResult());

    // The response should be successful even if libraries don't exist
    assertTrue("Get libraries should be successful", smcLibraryResult.isSuccess());
    assertNotNull("Libraries response should not be null", smcLibraryResult.getResult());

    for (Map.Entry<String, String> entry : smcLibraryResult.getResult().getLibraries().entrySet()) {

      Cell libCell = Cell.fromBoc(entry.getValue());
      log.info("cell lib {}", libCell.toHex());
    }
  }

  @Test
  public void testIfLibraryHasBeenDeployedAdnlLiteClient() throws Exception {
    AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().testnet().liteServerIndex(2).build();
    //    Cell walletV5Code =
    // CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    Cell walletV5Code = CellBuilder.beginCell().fromBoc(WalletCodes.V4R2.getValue()).endCell();

    LibraryResult smcLibraryResult =
        adnlLiteClient.getLibraries(Collections.singletonList(walletV5Code.getHash()));
    log.info("smcLibraryResult {}", smcLibraryResult);

    for (LibraryEntry lib : smcLibraryResult.getResult()) {
      Cell libCell = Cell.fromBoc(lib.data);
      log.info("cell lib {}", libCell.toHex());
    }
  }
}
