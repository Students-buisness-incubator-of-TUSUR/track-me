package net.akarmanov.projectplace.commons.filters;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OperationType {
  EQUAL("EQ"),
  LIKE("LIKE");

  private final String value;

  OperationType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
