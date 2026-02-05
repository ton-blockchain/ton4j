package org.ton.ton4j.smartcontract.dns;

import static java.util.Objects.nonNull;

import java.util.Map;
import lombok.Builder;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.provider.TonProvider;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

@Builder
public class Dns {

  public static final String DNS_CATEGORY_NEXT_RESOLVER =
      "dns_next_resolver"; // Smart Contract address
  public static final String DNS_CATEGORY_WALLET = "wallet"; // Smart Contract address
  public static final String DNS_CATEGORY_SITE = "site"; // ADNL address

  private TonProvider tonProvider;

  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private Tonlib tonlib;
  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private AdnlLiteClient adnlLiteClient;
  /** @deprecated Use tonProvider instead. */
  @Deprecated
  private TonCenter tonCenterClient;

  public Address getRootDnsAddress() {
    TonProvider provider = getTonProvider();
    if (provider instanceof TonCenter) {
      Map<String, Object> config =
          ((TonCenter) provider).getConfigParam(4, null).getResult().getConfig();
      String cellBase64 = (String) config.get("bytes");
      Cell cell = Cell.fromBoc(Utils.base64ToBytes(cellBase64));
      byte[] byteArray = cell.getBits().toByteArray();
      if (byteArray.length != 256 / 8) {
        throw new Error("Invalid ConfigParam 4 length " + byteArray.length);
      }
      String hex = Utils.bytesToHex(byteArray);
      return Address.of("-1:" + hex);
    } else if (provider instanceof AdnlLiteClient) {
      return Address.of("-1:" + ((AdnlLiteClient) provider).getConfigParam4().getDnsRootAddr());
    } else if (provider instanceof Tonlib) {
      Tonlib tonlibProvider = (Tonlib) provider;
      Cell cell = tonlibProvider.getConfigParam(tonlibProvider.getLast().getLast(), 4);
      byte[] byteArray = cell.getBits().toByteArray();
      if (byteArray.length != 256 / 8) {
        throw new Error("Invalid ConfigParam 4 length " + byteArray.length);
      }
      String hex = Utils.bytesToHex(byteArray);
      return Address.of("-1:" + hex);
    } else {
      throw new Error("provider not set");
    }
  }

  public Object resolve(String domain, String category, boolean oneStep) {
    Address rootDnsAddress = getRootDnsAddress();
    TonProvider provider = getTonProvider();
    if (provider instanceof AdnlLiteClient) {
      return DnsUtils.dnsResolve(
          (AdnlLiteClient) provider, rootDnsAddress, domain, category, oneStep);
    }
    if (provider instanceof TonCenter) {
      return DnsUtils.dnsResolve((TonCenter) provider, rootDnsAddress, domain, category, oneStep);
    }
    if (provider instanceof Tonlib) {
      return DnsUtils.dnsResolve((Tonlib) provider, rootDnsAddress, domain, category, oneStep);
    }
    throw new Error("provider not set");
  }

  public Object resolve(String domain, String category) {
    return resolve(domain, category, false);
  }

  /**
   * @param domain String e.g "sub.alice.ton"
   * @return Address | null
   */
  public Object getWalletAddress(String domain) {
    return resolve(domain, DNS_CATEGORY_WALLET);
  }

  /**
   * @param domain String e.g "sub.alice.ton"
   * @return AdnlAddress | null
   */
  public Object getSiteAddress(String domain) {
    return resolve(domain, DNS_CATEGORY_SITE);
  }

  private TonProvider getTonProvider() {
    if (nonNull(tonProvider)) {
      return tonProvider;
    }
    if (nonNull(tonCenterClient)) {
      return tonCenterClient;
    }
    if (nonNull(adnlLiteClient)) {
      return adnlLiteClient;
    }
    return tonlib;
  }
}
