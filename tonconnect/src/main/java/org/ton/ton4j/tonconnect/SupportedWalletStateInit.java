package org.ton.ton4j.tonconnect;

import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.codec.binary.Hex;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** Validates the initial state of supported standard wallets before using their signing key. */
final class SupportedWalletStateInit {
  private SupportedWalletStateInit() {}

  static byte[] resolvePublicKey(String canonicalAddress, String walletStateInit) {
    try {
      if (walletStateInit == null || walletStateInit.isEmpty()) {
        throw invalidState();
      }
      // TON Connect accepts both standard and URL-safe base64 BoCs.
      byte[] boc =
          Base64.getDecoder().decode(walletStateInit.replace('-', '+').replace('_', '/'));
      List<Cell> roots = Cell.fromBocMultiRoots(boc);
      if (roots.size() != 1) {
        throw invalidState();
      }
      Cell root = roots.get(0);
      requireOrdinaryCells(root);
      if (root.getBitLength() != 5 || root.getRefs().size() != 2) {
        throw invalidState();
      }
      CellSlice state = CellSlice.beginParse(root);
      // No split depth, special flags or libraries; code and data must both be present.
      if (state.loadUint(5).intValue() != 0b00110) {
        throw invalidState();
      }
      Cell code = state.loadRef();
      Cell data = state.loadRef();
      byte[] key = extractPublicKey(code, data);

      // Rebuild the validated StateInit so the address uses the actual code and data hashes.
      Cell validatedState =
          CellBuilder.beginCell().storeUint(0b00110, 5).storeRef(code).storeRef(data).endCell();
      String claimedHash = canonicalAddress.substring(canonicalAddress.indexOf(':') + 1);
      if (!claimedHash.equals(Hex.encodeHexString(root.getHash()))
          || !claimedHash.equals(Hex.encodeHexString(validatedState.getHash()))) {
        throw new IllegalArgumentException("Wallet StateInit does not match the account address");
      }
      return key;
    } catch (Error malformedCell) {
      // The cell parser reports malformed input with plain Error; VM/linkage failures must escape.
      if (malformedCell.getClass() != Error.class) {
        throw malformedCell;
      }
      throw new IllegalArgumentException("Invalid wallet StateInit", malformedCell);
    }
  }

  private static byte[] extractPublicKey(Cell code, Cell data) {
    // Official code hashes: https://docs.ton.org/contracts/standard/wallets/history
    String codeHash = Hex.encodeHexString(code.getHash());
    int offset;
    int dataBits;
    boolean dictionary;
    switch (codeHash) {
      case "a0cfc2c48aee16a271f2cfc0b7382d81756cecb1017d077faaab3bb602f6868c": // V1R1
      case "d4902fcc9fad74698fa8e353220a68da0dcf72e32bcb2eb9ee04217c17d3062c": // V1R2
      case "587cc789eff1c84f46ec3797e45fc809a14ff5ae24f1e0c7a6a99cc9dc9061ff": // V1R3
      case "5c9a5e68c108e18721a07c42f9956bfb39ad77ec6d624b60c576ec88eee65329": // V2R1
      case "fe9530d3243853083ef2ef0b4c2908c0abf6fa1c31ea243aacaa5bf8c7d753f1": // V2R2
        offset = 32;
        dataBits = 288;
        dictionary = false;
        break;
      case "b61041a58a7980b946e8fb9e198e3c904d24799ffa36574ea4251c41a566f581": // V3R1
      case "84dafa449f98a6987789ba232358072bc0f76dc4524002a5d0918b9a75d2d599": // V3R2
        offset = 64;
        dataBits = 320;
        dictionary = false;
        break;
      case "64dd54805522c5be8a9db59cea0105ccf0d08786ca79beb8cb79e880a8d7322d": // V4R1
      case "feb5ff6820e2ff0d9483e7e0d62c817d846789fb4ae580c878866d959dabd5c0": // V4R2
        offset = 64;
        dataBits = 321;
        dictionary = true;
        break;
      case "20834b7b72b112147e1b2fb457b84e74d1a30f04f737d4f62a668e9552d2b72f": // V5R1
        offset = 65;
        dataBits = 322;
        dictionary = true;
        break;
      default:
        throw new IllegalArgumentException("Unsupported wallet code in StateInit");
    }
    if (data.getBitLength() != dataBits || !data.getRefs().isEmpty()) {
      throw invalidState();
    }
    CellSlice contents = CellSlice.beginParse(data).skipBits(offset);
    byte[] key = contents.loadBytes(256);
    // This local verifier supports standard initial states with empty plugin/extension dictionaries.
    if (dictionary && contents.loadBit()) {
      throw invalidState();
    }
    if (contents.getRestBits() != 0 || contents.getRefsCount() != 0) {
      throw invalidState();
    }
    return key;
  }

  private static void requireOrdinaryCells(Cell root) {
    Set<Cell> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    ArrayDeque<Cell> pending = new ArrayDeque<>();
    pending.push(root);
    while (!pending.isEmpty()) {
      Cell cell = pending.pop();
      if (!visited.add(cell)) {
        continue;
      }
      if (cell.isExotic() || cell.levelMask.getMask() != 0) {
        throw invalidState();
      }
      pending.addAll(cell.getRefs());
    }
  }

  private static IllegalArgumentException invalidState() {
    return new IllegalArgumentException("Invalid or unsupported wallet StateInit layout");
  }
}
