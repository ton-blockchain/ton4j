package org.ton.ton4j.tonconnect;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tlb.StateInit;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonConnect {

  Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

  /**
   * <a
   * href="https://docs.ton.org/develop/dapps/ton-connect/sign#how-does-it-work">how-does-it-work</a>
   */
  @Test
  public void testTonConnect() throws Exception {

    AdnlLiteClient client = AdnlLiteClient.builder().myLocalTon().build();

    byte[] secretKey =
        Utils.hexToSignedBytes("1bd726fa69d850a5c0032334b16802c7eda48fde7a0e24f28011b22159cc97b7");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    log.info("prvKey: {}", Utils.bytesToHex(secretKey));
    log.info("pubKey: {}", Utils.bytesToHex(keyPair.getPublicKey()));

    String addressStr = "0:1da77f0269bbbb76c862ea424b257df63bd1acb0d4eb681b68c9aadfbf553b93";
    Address address = Address.of(addressStr);
    Account account = client.getAccount(address);
    log.info("account {}", account);

    // 0. User initiates a sign-in process at the dapp's frontend, then dapp sends a request to its
    // backend to generate ton_proof payload.
    // 1. Backend generates a TonProof entity and sends it to a frontend (without signature,
    // obviously)

    String payload = "doc-example-<BACKEND_AUTH_ID>";
    String sig = "";

    String proof =
        String.format(
            "{\n"
                + "                \"timestamp\": 1722999580, \n"
                + "                \"domain\": {\n"
                + "                    \"lengthBytes\": 16, \n"
                + "                    \"value\": \"xxx.xxx.com\"\n"
                + "                }, \n"
                + "                \"signature\": \"%s\", \n"
                + // <---------- to be updated
                "                \"payload\": \"%s\"\n"
                + "            }",
            sig, payload);

    log.info("proof: {}", proof);

    TonProof tonProof = gson.fromJson(proof, TonProof.class);

    byte[] message = TonConnect.createMessageForSigning(tonProof, addressStr);

    // 2. Frontend signs in to the wallet using TonProof and receives back a signed TonProof.
    // Basically
    //  a user signs the payload with his private key.

    byte[] signature = Utils.signData(keyPair.getPublicKey(), secretKey, message);
    log.info("signature: {}", Utils.bytesToHex(signature));

    // update TonProof by adding a signature
    tonProof.setSignature(Utils.bytesToBase64SafeUrl(signature));

    log.info("proof (updated): {}", tonProof);

    // 3. Frontend sends signed TonProof to a backend for verification.
    // If a smart-contract does not have a get_public_key method, you can calculate a public key from
    // its state init.
    StateInit stateInit =
        StateInit.builder()
            .code(account.getStateInit().getCode())
            .data(account.getStateInit().getData())
            .build();

    log.info("wallet code bocHex: {}", account.getStateInit().getCode().toHex());
    log.info(
        "wallet version {}", WalletCodes.getKeyByValue(account.getStateInit().getCode().toHex()));

    BigInteger publicKeyRemote = client.getPublicKey(address);
    // OR handover stateInit to calculate pubkey from contract's data

    String accountString =
        String.format(
            "{\n"
                + "        \"address\": \"%s\", \n"
                + "        \"chain\": \"-239\", \n"
                + "        \"walletStateInit\": \"%s\", \n" + // either this
                "        \"publicKey\": \"%s\"\n" +           // or this
                "    }",
            address.toRaw(), stateInit.toCell().toBase64(), publicKeyRemote.toString(16));

    log.info("accountString: {}", accountString);

    WalletAccount walletAccount = gson.fromJson(accountString, WalletAccount.class);

    log.info("account:{}", walletAccount);

    assertThat(TonConnect.checkProof(tonProof, walletAccount)).isTrue();
  }

  @Test
  public void testTonConnectExample() throws Exception {

    String addressStr = "0:2d29bfa071c8c62fa3398b661a842e60f04cb8a915fb3e749ef7c6c41343e16c";

    // backend prepares
    TonProof tonProof =
        TonProof.builder()
            .timestamp(1722999580)
            .domain(Domain.builder().value("xxx.xxx.com").lengthBytes(16).build())
            .payload("doc-example-<BACKEND_AUTH_ID>")
            .build();

    // wallet signs
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    byte[] message = TonConnect.createMessageForSigning(tonProof, addressStr);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), secretKey, message);
    log.info("signature: {}", Utils.bytesToHex(signature));

    // update TonProof by adding a signature
    tonProof.setSignature(Utils.bytesToBase64SafeUrl(signature));

    // backend verifies
    WalletAccount walletAccount =
        WalletAccount.builder()
            .chain(-239)
            .address(addressStr)
            .publicKey("82a0b2543d06fec0aac952e9ec738be56ab1b6027fc0c1aa817ae14b4d1ed2fb")
            .build();

    assertThat(TonConnect.checkProof(tonProof, walletAccount)).isTrue();
  }
}
