package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/**
 * Response for runGetMethodStd endpoint
 * This is the standardized version that uses TVM stack entries
 */
@Data
public class RunGetMethodStdResponse {
  @SerializedName("@type")
  private String type = "smc.runResult";

  @SerializedName("gas_used")
  private Long gasUsed;

  @SerializedName("stack")
  private List<Object> stack; // TVM stack entries

  @SerializedName("exit_code")
  private Integer exitCode;
}
