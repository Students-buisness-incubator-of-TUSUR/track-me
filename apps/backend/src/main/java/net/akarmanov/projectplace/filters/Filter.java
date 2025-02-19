package net.akarmanov.projectplace.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * Фильтр для запросов поиска
 *
 * @param fieldName имя поля
 * @param type      тип операции
 * @param values    значения
 */
@Builder
@Schema(description = "Фильтр для запросов поиска")
public record Filter(@NotBlank(message = "Имя поля не может быть пустым")
                     @Schema(description = "Имя поля")
                     String fieldName,
                     @Schema(description = "Тип операции",
                             implementation = OperationType.class,
                             allowableValues = {"EQ", "LIKE"})
                     @NotNull(message = "Тип операции не может быть пустым")
                     OperationType type,
                     @Schema(description = "Значения", defaultValue = "null")
                     List<String> values,
                     @Schema(description = "Значение",
                             name = "value") @JsonProperty("value")
                     String singleValue) {
  private static Path<?> getPath(Root<?> root, String[] fields) {
    From<?, ?> path = root;
    for (int i = 0; i < fields.length - 1; i++) {
      String field = fields[i];
      path = path.join(field);
    }
    return path.get(fields[fields.length - 1]);
  }

  public Predicate toPredicate(Root<?> root, CriteriaBuilder cb) {
    var partPath = fieldName.split("\\.");
    var path = partPath.length > 1 ? getPath(root, partPath) : root.get(fieldName);
    switch (type) {
      case EQUAL -> {
        if (values != null) {
          return path.as(String.class).in(values);
        } else {
          return cb.equal(path.as(String.class), singleValue);
        }
      }
      case LIKE -> {
        return cb.like(cb.lower(path.as(String.class)), "%" + singleValue.toLowerCase() + "%");
      }
      default -> throw new IllegalArgumentException("Неизвестный тип операции");
    }
  }
}
