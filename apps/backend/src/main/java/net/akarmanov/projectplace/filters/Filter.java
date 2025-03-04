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

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

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
  public static final String START_DATE_FIELD = "startDate";

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
        return resolvePredicate(cb, path);
      }
      case LIKE -> {
        return cb.like(cb.lower(path.as(String.class)), "%" + singleValue.toLowerCase() + "%");
      }
      default -> throw new IllegalArgumentException("Неизвестный тип операции");
    }
  }

  private Predicate resolvePredicate(CriteriaBuilder cb, Path<?> path) {
    var partPath = fieldName.split("\\.");
    if (isEmpty(values)) {
      if (START_DATE_FIELD.equals(partPath[partPath.length - 1])) {
        return getPredicate(cb, path, singleValue);
      }
      return cb.equal(path.as(String.class), singleValue);
    } else {
      if (START_DATE_FIELD.equals(partPath[partPath.length - 1])) {
        var predicates = values.stream()
            .map(s -> getPredicate(cb, path, s))
            .toList();
        return cb.or(predicates.toArray(new Predicate[0]));
      }
      return path.as(String.class).in(values);
    }
  }

  private Predicate getPredicate(CriteriaBuilder cb, Path<?> path, String singleValue) {
    var year = Year.parse(singleValue);
    var start = LocalDate.of(year.getValue(), Month.JANUARY, 1);
    var end = LocalDate.of(year.getValue() + 1, Month.JANUARY, 1);
    return cb.between(path.as(LocalDate.class), start, end);
  }
}
