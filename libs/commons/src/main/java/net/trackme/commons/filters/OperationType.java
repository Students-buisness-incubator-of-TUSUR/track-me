package net.trackme.commons.filters;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OperationType", description = "Тип операции фильтрации")
public enum OperationType {
    @Schema(description = "Равенство")
    EQUALS("EQ"),
    @Schema(description = "Больше чем")
    GREATER_THAN("GT"),
    @Schema(description = "Больше или равно")
    GREATER_THAN_OR_EQUAL("GTE"),
    @Schema(description = "Проверка на вхождение в список")
    IN("IN"),
    @Schema(description = "Проверка на наличие значения")
    IS_NOT_NULL("IS_NOT_NULL"),
    @Schema(description = "Проверка на отсутствие значения")
    IS_NULL("IS_NULL"),
    @Schema(description = "Меньше чем")
    LESS_THAN("LT"),
    @Schema(description = "Меньше или равно")
    LESS_THAN_OR_EQUAL("LTE"),
    @Schema(description = "Проверка на вхождение в строку")
    LIKE("LIKE"),
    @Schema(description = "Проверка на вхождение в список с учетом регистра")
    NOT_EQUALS("NEQ"),
    @Schema(description = "Проверка на отсутствие в списке")
    NOT_IN("NIN"),
    @Schema(description = "Проверка на отсутствие вхождения в строку")
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
