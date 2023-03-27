package com.aa.isolation.test;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Builder
public class UpdateInfo {

  private String column;
  private String setValue;
  private String whereValue;

}
