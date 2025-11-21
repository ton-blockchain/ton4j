package org.ton.ton4j.tlb.print;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
@Slf4j
public class MessagePrintInfo {
  String
      hash; // for in msg (inMsg.normalizedHash(), for outMsg - outMsg.toCell().getHash() - both in
  // base64
  String direction;
  String type;
  String op;
  BigInteger fwdFee;
  BigInteger value;
  BigInteger extraFlags;
  BigInteger createdAt;
  BigInteger createdLt;
  BigInteger importFee;
  String src;
  String dst;
  String comment;

  private static String header =
      "| msgHash | in/out | type         | op        | value           | fwdFee          | extraFlags      | importFee       | timestamp           | lt             | src             | dst             | comment                    |";

  public void printMessageInfo() {
    MessagePrintInfo msgFee = this;

    String str =
        String.format(
            "| %-8s| %-7s| %-13s| %-10s| %-16s| %-16s| %-16s| %-16s| %-20s| %-15s| %-16s| %-16s| %-27s|",
            msgFee.getHash().substring(0, 7),
            msgFee.getDirection(),
            msgFee.getType(),
            msgFee.getOp(),
            isNull(msgFee.getValue()) ? "" : Utils.formatNanoValueZeroStripZeros(msgFee.getValue()),
            isNull(msgFee.getFwdFee())
                ? ""
                : Utils.formatNanoValueZeroStripZeros(msgFee.getFwdFee()),
            isNull(msgFee.getExtraFlags())
                ? ""
                : Utils.formatNanoValueZeroStripZeros(msgFee.getExtraFlags()),
            isNull(msgFee.getImportFee())
                ? ""
                : Utils.formatNanoValueZeroStripZeros(msgFee.getImportFee()),
            isNull(msgFee.getCreatedAt())
                ? ""
                : (msgFee.getCreatedAt().longValue() == 0)
                    ? "0"
                    : Utils.toUTC(msgFee.getCreatedAt().longValue()),
            isNull(msgFee.getCreatedLt()) ? "" : msgFee.getCreatedLt().toString(),
            getShortSourceAddress(),
            getShortDestinationAddress(),
            msgFee.getComment());
    System.out.println("    " + str);
  }

  private static String getAccountAddrShort(String accountAddr) {
    if (nonNull(accountAddr)) {
      String str64 = StringUtils.leftPad(accountAddr, 64, "0");
      return str64.substring(0, 7) + "..." + str64.substring(str64.length() - 5);
    } else {
      return "N/A";
    }
  }

  public void printMessageInfo(Map<String, String> addrLabelMap) {
    MessagePrintInfo msgFee = this;

    String shortSrc = getShortSourceAddress();
    String shortDst = getShortDestinationAddress();

    for (Map.Entry<String, String> entry : addrLabelMap.entrySet()) {
      if (getAccountAddrShort(entry.getKey()).equals(shortSrc)) {
        shortSrc = entry.getValue();
      }
    }

    if (shortSrc.length() > 15) shortSrc = shortSrc.substring(0, 15);

    for (Map.Entry<String, String> entry : addrLabelMap.entrySet()) {
      if (getAccountAddrShort(entry.getKey()).equals(shortDst)) {
        shortDst = entry.getValue();
      }
    }
    if (shortDst.length() > 15) shortDst = shortDst.substring(0, 15);

    String str =
        String.format(
            "| %-8s| %-7s| %-13s| %-10s| %-16s| %-16s| %-16s| %-16s| %-20s| %-15s| %-16s| %-16s| %-27s|",
            msgFee.getHash().substring(0, 7),
            msgFee.getDirection(),
            msgFee.getType(),
            msgFee.getOp(),
            isNull(msgFee.getValue()) ? "" : Utils.formatNanoValueZeroStripZeros(msgFee.getValue()),
            isNull(msgFee.getFwdFee())
                ? ""
                : Utils.formatNanoValueZeroStripZeros(msgFee.getFwdFee()),
            isNull(msgFee.getExtraFlags())
                ? ""
                : Utils.formatNanoValueZeroStripZeros(msgFee.getExtraFlags()),
            isNull(msgFee.getImportFee())
                ? ""
                : Utils.formatNanoValueZeroStripZeros(msgFee.getImportFee()),
            isNull(msgFee.getCreatedAt())
                ? ""
                : (msgFee.getCreatedAt().longValue() == 0)
                    ? "0"
                    : Utils.toUTC(msgFee.getCreatedAt().longValue()),
            isNull(msgFee.getCreatedLt()) ? "" : msgFee.getCreatedLt().toString(),
            shortSrc,
            shortDst,
            msgFee.getComment());
    System.out.println("    " + str);
  }

  public static void printMessageInfoHeader() {
    //    System.out.println("");
    //    System.out.println("Messages");
    System.out.println(
        "    "
            + StringUtils.repeat("-", header.length() / 2)
            + " Messages "
            + StringUtils.repeat("-", (header.length() / 2) - 9));
    System.out.println("    " + header);
    System.out.println("    " + StringUtils.repeat("-", header.length()));
  }

  public static void printMessageInfoFooter() {
    System.out.println("    " + StringUtils.repeat("-", header.length()));
  }

  public String getShortSourceAddress() {
    if (StringUtils.isNotEmpty(src)) {
      return src.substring(0, 7) + "..." + src.substring(src.length() - 5);
    } else {
      return "";
    }
  }

  public String getShortDestinationAddress() {
    if (StringUtils.isNotEmpty(dst)) {
      return dst.substring(0, 7) + "..." + dst.substring(dst.length() - 5);
    } else {
      return "";
    }
  }
}
