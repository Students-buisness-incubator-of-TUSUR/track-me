package net.trackme.commons.filters;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OperationType {
    EQUALS("EQ"),
    GREATER_THAN("GT"),
    GREATER_THAN_OR_EQUAL("GTE"),
    IN("IN"),
    IS_NOT_NULL("IS_NOT_NULL"),
    IS_NULL("IS_NULL"),
    LESS_THAN("LT"),
    LESS_THAN_OR_EQUAL("LTE"),
    LIKE("LIKE"),
    NOT_EQUALS("NEQ"),
    NOT_IN("NIN"),
    NOT_LIKE("NOT_LIKE");

    private final String value;

    OperationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
