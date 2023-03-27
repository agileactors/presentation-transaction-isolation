package com.aa.isolation;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Builder
public class ReadDelayedInfo {

  private Object initialValue;
  private Object delayedValue;

}
