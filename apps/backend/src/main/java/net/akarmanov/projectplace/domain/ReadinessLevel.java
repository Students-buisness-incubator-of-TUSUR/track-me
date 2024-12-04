package net.akarmanov.projectplace.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ReadinessLevel {
    LEVEL_1("0-2"),
    LEVEL_2("3-5"),
    LEVEL_3("6-8"),
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
}
