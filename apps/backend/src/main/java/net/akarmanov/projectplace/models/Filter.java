package net.akarmanov.projectplace.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * Фильтр для запросов поиска
 *
 * @param fieldName     имя поля
 * @param operationType тип операции
 * @param values        значения
 */
@Builder
@Schema(description = "Фильтр для запросов поиска")
public record Filter(
    @NotBlank(message = "Имя поля не может быть пустым")
    @Schema(description = "Имя поля")
    String fieldName,
    @Schema(description = "Имя поля для объединения")
    String joinFieldName,
    @Schema(description = "Тип операции",
            implementation = OperationType.class)
    @NotNull(message = "Тип операции не может быть пустым")
    OperationType operationType,
    @Schema(description = "Значения")
    List<String> values,
    @Schema(description = "Значение",
            name = "value")
    @JsonProperty("value")
    String singleValue
) {
  public Predicate toPredicate(Root<?> root,
                               CriteriaBuilder criteriaBuilder) {
    if (joinFieldName != null) {
      return switch (operationType) {
        case EQUAL ->
            criteriaBuilder.equal(root.join(joinFieldName).get(fieldName).as(String.class),
                singleValue);
        case IN -> root.join(joinFieldName).get(fieldName).as(String.class).in(values);
        case LIKE -> criteriaBuilder.like(root.join(joinFieldName).get(fieldName).as(String.class),
            "%" + singleValue + "%");
      };
    } else {
      return switch (operationType) {
        case EQUAL -> criteriaBuilder.equal(root.get(fieldName).as(String.class), singleValue);
        case IN -> root.get(fieldName).as(String.class).in(values);
        case LIKE ->
            criteriaBuilder.like(root.get(fieldName).as(String.class), "%" + singleValue + "%");
      };
    }
  }
}
