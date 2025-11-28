package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/**
 * Response for getTransactionsStd endpoint
 * This is the standardized version that wraps transactions with additional metadata
 */
@Data
public class TransactionsStdResponse {
  @SerializedName("@type")
  private String type = "raw.transactions";

  @SerializedName("transactions")
  private List<TransactionResponse> transactions;

  @SerializedName("previous_transaction_id")
  private InternalTransactionId previousTransactionId;

  @Data
  public static class InternalTransactionId {
    @SerializedName("@type")
    private String type = "internal.transactionId";

    @SerializedName("lt")
    private String lt;

    @SerializedName("hash")
    private String hash;
  }
}
