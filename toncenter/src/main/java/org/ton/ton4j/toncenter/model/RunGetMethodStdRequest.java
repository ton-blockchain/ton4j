package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Request for runGetMethodStd endpoint
 * This is the standardized version that uses TVM stack entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunGetMethodStdRequest {
  @SerializedName("address")
  private String address;

  @SerializedName("method")
  private Object method; // Can be String or Integer

  @SerializedName("stack")
  private List<Object> stack; // TVM stack entries

  @SerializedName("seqno")
  private Long seqno;

  public RunGetMethodStdRequest(String address, Object method, List<Object> stack) {
    this.address = address;
    this.method = method;
    this.stack = stack;
  }
}
