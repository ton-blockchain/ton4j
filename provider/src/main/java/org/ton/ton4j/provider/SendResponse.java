package org.ton.ton4j.provider;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SendResponse {
  long code;
  String message;
}
