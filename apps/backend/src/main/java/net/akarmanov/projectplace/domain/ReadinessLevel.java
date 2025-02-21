package net.akarmanov.projectplace.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

@Schema(description = "Уровень готовности технологии", allowableValues = {"0-2", "3-5", "6-8", "9-10"})
public enum ReadinessLevel {
  @Schema(description = "0-2")
  LEVEL_1("0-2"),
  @Schema(description = "3-5")
  LEVEL_2("3-5"),
  @Schema(description = "6-8")
  LEVEL_3("6-8"),
  @Schema(description = "9-10")
  LEVEL_4("9-10");

  private static final Map<String, ReadinessLevel> LEVEL_MAP = new HashMap<>();


  static {
    for (ReadinessLevel level : ReadinessLevel.values()) {
      LEVEL_MAP.put(level.getValue(), level);
    }
  }

  private final String value;

  ReadinessLevel(String value) {
    this.value = value;
  }

  public static ReadinessLevel fromValue(String value) {
    return LEVEL_MAP.get(value);
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
