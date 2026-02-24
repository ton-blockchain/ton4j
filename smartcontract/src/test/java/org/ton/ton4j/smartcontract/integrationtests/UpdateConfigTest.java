package org.ton.ton4j.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.provider.SendResponse;
import org.ton.ton4j.smartcontract.GenericSmartContract;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.FullAccountState;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class UpdateConfigTest {

  @Test
  public void testMainWalletMyLocalTon() throws Exception {
    byte[] prvKey = Utils.downloadFile("http://localhost:8000/main-wallet.pk");
    log.info("prvKeyHex {}", Utils.bytesToHex(prvKey));

    TweetNaclFast.Signature.KeyPair keyPairSignature =
        Utils.generateSignatureKeyPairFromSeed(prvKey);
    AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().myLocalTon().build();

    log.info("minter addr {}", adnlLiteClient.getConfigParam2());

    Account account =
        adnlLiteClient.getAccount(
            Address.of("-1:ee8bd22f43f56e50e0f914cedbd0594ece7ed7a3e8131b73862cab98294c4a21"));
    log.info("account {}", account);

    log.info(
        "seqno {}",
        adnlLiteClient.getSeqno(
            Address.of("-1:ee8bd22f43f56e50e0f914cedbd0594ece7ed7a3e8131b73862cab98294c4a21")));

    WalletV3R2 mainWallet =
        WalletV3R2.builder()
            .tonProvider(adnlLiteClient)
            .walletId(42)
            .wc(-1)
            .keyPair(keyPairSignature)
            .initialSeqno(0)
            .build();

    log.info("mainWallet addr {}", mainWallet.getAddress().toRaw());
    log.info("mainWallet seqno {}", mainWallet.getSeqno());
  }

  @Test
  public void testConfigWalletMyLocalTon() throws Exception {
    byte[] prvKey = Utils.downloadFile("http://localhost:8000/config-master.pk");
    log.info("prvKeyHex {}", Utils.bytesToHex(prvKey));

    TweetNaclFast.Signature.KeyPair keyPairSignature =
        Utils.generateSignatureKeyPairFromSeed(prvKey);
    AdnlLiteClient adnlLiteClient = AdnlLiteClient.builder().myLocalTon().build();

    long seqno =
        adnlLiteClient.getSeqno(
            Address.of("-1:5555555555555555555555555555555555555555555555555555555555555555"));
    log.info("seqno {}", seqno);

    log.info("config param 2 {}", adnlLiteClient.getConfigParam2());

    Account account =
        adnlLiteClient.getAccount(
            Address.of("-1:5555555555555555555555555555555555555555555555555555555555555555"));

    GenericSmartContract configSmartContract =
        GenericSmartContract.builder()
            .tonProvider(adnlLiteClient)
            .wc(-1)
            .customAddress(
                Address.of("-1:5555555555555555555555555555555555555555555555555555555555555555"))
            .code(account.getStateInit().getCode().toHex())
            .data(account.getStateInit().getData().toHex())
            .keyPair(keyPairSignature)
            .build();
    log.info("smc config addr  {}", configSmartContract.getAddress((byte) -1).toRaw());

    Cell param = ConfigParams2.builder().minterAddr(BigInteger.TEN).build().toCell();
    Cell body =
        CellBuilder.beginCell()
            .storeUint(0x43665021, 32) // id
            .storeUint(seqno, 32) // seqno
            .storeUint(Utils.now() + 60, 32) // valid until
            .storeUint(2, 32) // idx
            .storeRef(param)
            .endCell();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPairSignature,
            Address.of("-1:5555555555555555555555555555555555555555555555555555555555555555"),
            null,
            body);

    SendResponse sendResponse = configSmartContract.send(msg);
    log.info("sendResponse {}", sendResponse);
  }
}
