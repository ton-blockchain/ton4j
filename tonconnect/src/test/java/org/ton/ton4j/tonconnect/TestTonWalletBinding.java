package org.ton.ton4j.tonconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import com.iwebpp.crypto.TweetNaclFast;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellType;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.tlb.StateInit;
import org.ton.ton4j.tlb.TickTock;
import org.ton.ton4j.tonconnect.TestTonProofVerification.Fixture;
import org.ton.ton4j.utils.Utils;

public class TestTonWalletBinding {

  private static final WalletCodes[] SUPPORTED_WALLETS = {
    WalletCodes.V1R1, WalletCodes.V1R2, WalletCodes.V1R3,
    WalletCodes.V2R1, WalletCodes.V2R2,
    WalletCodes.V3R1, WalletCodes.V3R2, WalletCodes.V4R2, WalletCodes.V5R1
  };

  @Test
  public void attackerKeyCannotAuthenticateVictimAddressWithoutStateInit() throws Exception {
    Fixture victim = new Fixture();
    TweetNaclFast.Signature.KeyPair attacker = TweetNaclFast.Signature.keyPair();
    victim.account.setPublicKey(Utils.bytesToHex(attacker.getPublicKey()));
    victim.account.setWalletStateInit(null);
    victim.sign(attacker);

    assertThat(TonConnect.verifyProofSignature(victim.proof, victim.account)).isTrue();
    assertRejectedWithoutConsumption(victim);
  }

  @Test
  public void attackerStateInitCannotAuthenticateAnotherWalletAddress() throws Exception {
    Fixture victim = new Fixture();
    Fixture attacker = new Fixture();
    victim.account.setPublicKey(attacker.account.getPublicKey());
    victim.account.setWalletStateInit(attacker.account.getWalletStateInit());
    victim.sign(attacker.keyPair);

    assertThat(TonConnect.verifyProofSignature(victim.proof, victim.account)).isTrue();
    assertRejectedWithoutConsumption(victim);
  }

  @Test
  public void rejectsClientKeyThatDiffersFromAddressBoundStateInitKey() throws Exception {
    Fixture victim = new Fixture();
    TweetNaclFast.Signature.KeyPair attacker = TweetNaclFast.Signature.keyPair();
    victim.account.setPublicKey(Utils.bytesToHex(attacker.getPublicKey()));
    victim.sign(attacker);

    assertThat(TonConnect.verifyProofSignature(victim.proof, victim.account)).isTrue();
    assertRejectedWithoutConsumption(victim);
    victim.account.setPublicKey(null);
    assertRejectedWithoutConsumption(victim);
    victim.account.setPublicKey(Utils.bytesToHex(victim.keyPair.getPublicKey()));
    victim.sign();
    assertThat(victim.verify()).isTrue();
  }

  @Test
  public void verifiesEverySupportedWalletUsingItsLayoutAndDerivedAddress() throws Exception {
    for (WalletCodes version : SUPPORTED_WALLETS) {
      Fixture withClientKey = new Fixture(version, 0);
      assertThat(withClientKey.verify()).as("%s with client key", version).isTrue();

      Fixture extractedKey = new Fixture(version, 0);
      extractedKey.account.setPublicKey(null);
      assertThat(extractedKey.verify()).as("%s without client key", version).isTrue();
    }
  }

  @Test
  public void acceptsMasterchainAddressAndCaseInsensitiveClientHexKey() throws Exception {
    Fixture fixture = new Fixture(WalletCodes.V4R2, -1);
    fixture.account.setAddress(fixture.address.toUpperCase(Locale.ROOT));
    fixture.account.setPublicKey(fixture.account.getPublicKey().toUpperCase(Locale.ROOT));
    fixture.sign();

    assertThat(fixture.verify()).isTrue();
  }

  @Test
  public void acceptsStandardAndUrlSafeStateInitBocEncoding() throws Exception {
    Fixture standard = new Fixture();
    standard.account.setWalletStateInit(standard.stateInit.toCell().toBase64());
    assertThat(standard.verify()).isTrue();

    Fixture urlSafe = new Fixture();
    urlSafe.account.setWalletStateInit(urlSafe.stateInit.toCell().toBase64UrlSafe());
    assertThat(urlSafe.verify()).isTrue();
  }

  @Test
  public void rejectsUnknownWalletCodeEvenWhenStateInitHashMatchesAddress() throws Exception {
    Fixture fixture = new Fixture();
    Cell state =
        StateInit.builder()
            .code(CellBuilder.beginCell().storeUint(42, 8).endCell())
            .data(fixture.stateInit.getData())
            .build()
            .toCell();
    claimState(fixture, state);

    assertThat(TonConnect.verifyProofSignature(fixture.proof, fixture.account)).isTrue();
    assertRejectedWithoutConsumption(fixture);
  }

  @Test
  public void rejectsMalformedStateInitEncodingWithoutConsumingChallenge() throws Exception {
    for (String state : new String[] {null, "", "%%%", "AA==", "te6ccgEBAQA="}) {
      Fixture fixture = new Fixture();
      fixture.account.setWalletStateInit(state);
      assertRejectedWithoutConsumption(fixture);
    }
  }

  @Test
  public void rejectsInvalidStateInitFieldsAndTrailingRootContent() throws Exception {
    Fixture fixture = new Fixture();
    Cell code = fixture.stateInit.getCode();
    Cell data = fixture.stateInit.getData();
    Cell empty = CellBuilder.beginCell().endCell();
    List<Cell> invalidStates = new ArrayList<>();
    invalidStates.add(StateInit.builder().data(data).build().toCell());
    invalidStates.add(StateInit.builder().code(code).build().toCell());
    invalidStates.add(
        StateInit.builder().code(code).data(data).depth(BigInteger.ZERO).build().toCell());
    invalidStates.add(
        StateInit.builder()
            .code(code)
            .data(data)
            .tickTock(TickTock.builder().tick(true).tock(false).build())
            .build()
            .toCell());
    invalidStates.add(StateInit.builder().code(code).data(data).lib(empty).build().toCell());
    invalidStates.add(CellBuilder.beginCell().storeBits("00").endCell());
    invalidStates.add(
        CellBuilder.beginCell().storeCell(fixture.stateInit.toCell()).storeBit(false).endCell());
    invalidStates.add(
        CellBuilder.beginCell().storeCell(fixture.stateInit.toCell()).storeRef(empty).endCell());
    invalidStates.add(exoticLibrary());
    invalidStates.add(StateInit.builder().code(exoticLibrary()).data(data).build().toCell());
    invalidStates.add(StateInit.builder().code(code).data(exoticLibrary()).build().toCell());

    for (Cell state : invalidStates) {
      claimState(fixture, state);
      assertRejectedWithoutConsumption(fixture);
    }
  }

  @Test
  public void rejectsMultipleStateInitBocRoots() throws Exception {
    Fixture fixture = new Fixture();
    Cell state = fixture.stateInit.toCell();
    byte[] multipleRoots =
        state.toBocMultiRoot(
            new ArrayList<>(Arrays.asList(state, CellBuilder.beginCell().storeBit(true).endCell())),
            true,
            false,
            false,
            false,
            false);
    fixture.account.setWalletStateInit(Utils.bytesToBase64(multipleRoots));

    assertRejectedWithoutConsumption(fixture);
  }

  @Test
  public void rejectsTruncatedTrailingOrReferencedWalletDataForEveryVersion() throws Exception {
    for (WalletCodes version : SUPPORTED_WALLETS) {
      Fixture fixture = new Fixture(version, 0);
      String dataBits = fixture.stateInit.getData().getBits().toBitString();
      Cell[] invalidData = {
        CellBuilder.beginCell().storeBits(dataBits.substring(0, dataBits.length() - 1)).endCell(),
        CellBuilder.beginCell().storeBits(dataBits).storeBit(false).endCell(),
        CellBuilder.beginCell()
            .storeBits(dataBits)
            .storeRef(CellBuilder.beginCell().endCell())
            .endCell()
      };
      for (Cell data : invalidData) {
        claimState(
            fixture, StateInit.builder().code(fixture.stateInit.getCode()).data(data).build().toCell());
        assertRejectedWithoutConsumption(fixture);
      }
    }
  }

  @Test
  public void rejectsNonemptyPluginAndExtensionDictionaryFlags() throws Exception {
    for (WalletCodes version : new WalletCodes[] {WalletCodes.V4R2, WalletCodes.V5R1}) {
      Fixture fixture = new Fixture(version, 0);
      String dataBits = fixture.stateInit.getData().getBits().toBitString();
      Cell data =
          CellBuilder.beginCell()
              .storeBits(dataBits.substring(0, dataBits.length() - 1))
              .storeBit(true)
              .endCell();
      claimState(
          fixture, StateInit.builder().code(fixture.stateInit.getCode()).data(data).build().toCell());
      assertRejectedWithoutConsumption(fixture);
    }
  }

  @Test
  public void trustedKeyResolverReceivesCanonicalAddressAndExpectedChain() throws Exception {
    Fixture fixture = new Fixture(WalletCodes.V5R1, -1);
    fixture.account.setAddress(fixture.address.toUpperCase(Locale.ROOT));
    fixture.account.setPublicKey(null);
    fixture.account.setWalletStateInit("untrusted data is not a trusted key source");
    fixture.sign();
    AtomicInteger lookups = new AtomicInteger();
    TrustedWalletKeyResolver resolver =
        (address, chain) -> {
          assertThat(address).isEqualTo(fixture.address);
          assertThat(chain).isEqualTo(fixture.context().getExpectedChain());
          lookups.incrementAndGet();
          return fixture.keyPair.getPublicKey();
        };

    assertThat(verifyTrusted(fixture, resolver)).isTrue();
    assertThat(verifyTrusted(fixture, resolver)).isFalse();
    assertThat(lookups).hasValue(2);
  }

  @Test
  public void trustedResolverRejectsAttackerSignatureForVictimAddress() throws Exception {
    Fixture victim = new Fixture();
    TweetNaclFast.Signature.KeyPair attacker = TweetNaclFast.Signature.keyPair();
    victim.account.setPublicKey(Utils.bytesToHex(attacker.getPublicKey()));
    victim.account.setWalletStateInit(null);
    victim.sign(attacker);

    assertThat(verifyTrusted(victim, (address, chain) -> victim.keyPair.getPublicKey())).isFalse();
    assertThat(victim.store.calls).hasValue(0);
    victim.account.setPublicKey(null);
    assertThat(verifyTrusted(victim, (address, chain) -> victim.keyPair.getPublicKey())).isFalse();
    assertThat(victim.store.calls).hasValue(0);
    victim.sign();
    assertThat(verifyTrusted(victim, (address, chain) -> victim.keyPair.getPublicKey())).isTrue();
  }

  @Test
  public void rejectsMissingOrMalformedTrustedKeyWithoutConsumingChallenge() throws Exception {
    for (byte[] key : new byte[][] {null, new byte[0], new byte[31], new byte[33]}) {
      Fixture fixture = new Fixture();
      assertThat(verifyTrusted(fixture, (address, chain) -> key)).isFalse();
      assertThat(fixture.store.calls).hasValue(0);
    }
  }

  @Test
  public void trustedResolverFailureDoesNotConsumeChallenge() throws Exception {
    Fixture fixture = new Fixture();
    IOException failure = new IOException("trusted chain lookup unavailable");

    assertThat(
            assertThrows(
                IOException.class,
                () ->
                    verifyTrusted(
                        fixture,
                        (address, chain) -> {
                          throw failure;
                        })))
        .isSameAs(failure);
    assertThat(fixture.store.calls).hasValue(0);
    assertThat(verifyTrusted(fixture, (address, chain) -> fixture.keyPair.getPublicKey())).isTrue();
  }

  @Test
  public void invalidContextDoesNotInvokeTrustedResolver() throws Exception {
    Fixture fixture = new Fixture();
    fixture.account.setChain(-3);
    AtomicInteger lookups = new AtomicInteger();

    assertThat(
            verifyTrusted(
                fixture,
                (address, chain) -> {
                  lookups.incrementAndGet();
                  return fixture.keyPair.getPublicKey();
                }))
        .isFalse();
    assertThat(lookups).hasValue(0);
    assertThat(fixture.store.calls).hasValue(0);
  }

  private static boolean verifyTrusted(Fixture fixture, TrustedWalletKeyResolver resolver)
      throws Exception {
    return TonConnect.verifyProofWithTrustedKey(
        fixture.proof, fixture.account, fixture.context(), fixture.store, resolver);
  }

  private static void assertRejectedWithoutConsumption(Fixture fixture) throws Exception {
    assertThat(fixture.verify()).isFalse();
    assertThat(fixture.store.calls).hasValue(0);
    assertThat(fixture.store.challenges).containsKey(fixture.challenge);
  }

  private static void claimState(Fixture fixture, Cell state) throws Exception {
    fixture.account.setWalletStateInit(state.toBase64());
    fixture.account.setAddress("0:" + Utils.bytesToHex(state.getHash()));
    fixture.sign();
  }

  private static Cell exoticLibrary() {
    return CellBuilder.beginCell()
        .setExotic(true)
        .cellType(CellType.LIBRARY)
        .storeUint(2, 8)
        .storeBytes(new byte[32])
        .endCell();
  }
}
