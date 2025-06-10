package net.trackme.commons.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Фильтр для запросов поиска
 *
 * @param fieldName имя поля
 * @param type      тип операции
 * @param values    значения
 */
@Builder
@Schema(name = "Filter", description = "Фильтр для запросов поиска")
public record Filter(
        @Schema(description = "Имя поля для фильтрации", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Имя поля не может быть пустым")
        String fieldName,
        @NotNull(message = "Тип операции не может быть пустым")
        @Schema(description = "Тип операции фильтрации", requiredMode = Schema.RequiredMode.REQUIRED, implementation = OperationType.class)
        OperationType type,
        @Schema(description = "Список значений для фильтрации")
        List<String> values,
        @JsonProperty("value")
        @Schema(description = "Единственное значение для фильтрации")
        String singleValue) {

    public static final Map<Class<?>, Function<String, Object>> TYPE_CONVERTERS = Map.ofEntries(
            Map.entry(Boolean.class, Boolean::parseBoolean),
            Map.entry(boolean.class, Boolean::parseBoolean),
            Map.entry(Integer.class, Integer::parseInt),
            Map.entry(int.class, Integer::parseInt),
            Map.entry(Long.class, Long::parseLong),
            Map.entry(long.class, Long::parseLong),
            Map.entry(Double.class, Double::parseDouble),
            Map.entry(double.class, Double::parseDouble),
            Map.entry(Float.class, Float::parseFloat),
            Map.entry(float.class, Float::parseFloat),
            Map.entry(BigDecimal.class, BigDecimal::new),
            Map.entry(LocalDate.class, LocalDate::parse),
            Map.entry(LocalDateTime.class, LocalDateTime::parse),
            Map.entry(Instant.class, Instant::parse)
    );

    public Predicate toPredicate(Root<?> root, CriteriaBuilder cb) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }

        // Проверяем, заканчивается ли путь на .year или это просто year
        if (fieldName.endsWith(".year") || fieldName.equals("year")) {
            return createYearPredicate(root, cb);
        }

        // Получаем Path к полю, поддерживая вложенные пути
        var fieldPath = getFieldPath(root, fieldName);

        // Получаем значения для предиката
        var valuesList = getValues();

        if (valuesList.isEmpty() && !isNullOperation()) {
            throw new IllegalArgumentException("No values provided for filter");
        }

        // Определяем тип поля
        var fieldType = fieldPath.getJavaType();

        var first = valuesList.getFirst();

        return switch (type) {
            case EQUALS -> createEqualsPredicate(cb, fieldPath, first, fieldType);
            case NOT_EQUALS -> cb.not(createEqualsPredicate(cb, fieldPath, first, fieldType));
            case IN -> createInPredicate(fieldPath, valuesList, fieldType);
            case NOT_IN -> cb.not(createInPredicate(fieldPath, valuesList, fieldType));
            case GREATER_THAN -> createComparisonPredicate(cb, fieldPath, first, fieldType, ComparisonType.GREATER);
            case GREATER_THAN_OR_EQUAL ->
                    createComparisonPredicate(cb, fieldPath, first, fieldType, ComparisonType.GREATER_OR_EQUAL);
            case LESS_THAN -> createComparisonPredicate(cb, fieldPath, first, fieldType, ComparisonType.LESS);
            case LESS_THAN_OR_EQUAL ->
                    createComparisonPredicate(cb, fieldPath, first, fieldType, ComparisonType.LESS_OR_EQUAL);
            case LIKE -> cb.like(fieldPath.as(String.class), "%" + first + "%");
            case NOT_LIKE -> cb.notLike(fieldPath.as(String.class), "%" + first + "%");
            case IS_NULL -> cb.isNull(fieldPath);
            case IS_NOT_NULL -> cb.isNotNull(fieldPath);
        };
    }

    private Predicate createYearPredicate(Root<?> root, CriteriaBuilder cb) {
        var yearValues = getValues();

        if (yearValues.isEmpty()) {
            throw new IllegalArgumentException("No year values provided");
        }

        // Определяем путь к дате, заменяя year на startDate
        var datePath = fieldName;
        if (fieldName.endsWith(".year")) {
            // Заменяем .year на .startDate
            datePath = fieldName.substring(0, fieldName.length() - 5) + ".startDate";
        } else if (fieldName.equals("year")) {
            // Если просто year, то ищем startDate
            datePath = "startDate";
        }

        Path<LocalDate> dateFieldPath;
        try {
            //noinspection unchecked
            dateFieldPath = (Path<LocalDate>) getFieldPath(root, datePath);
        } catch (Exception e) {
            // Если startDate не найден, пробуем с date
            var alternativePath = datePath.replace("startDate", "date");
            try {
                //noinspection unchecked
                dateFieldPath = (Path<LocalDate>) getFieldPath(root, alternativePath);
            } catch (Exception e2) {
                throw new IllegalArgumentException(
                        String.format("No date field found for year filter. Tried: %s and %s", datePath, alternativePath), e2);
            }
        }

        // Создаем предикаты для каждого года
        var finalDateFieldPath = dateFieldPath;
        var yearPredicates = yearValues.stream()
                .map(yearStr -> {
                    var year = Year.parse(yearStr);
                    var startOfYear = year.atMonth(Month.JANUARY).atDay(1);
                    var endOfYear = year.atMonth(Month.DECEMBER).atEndOfMonth();
                    return cb.between(finalDateFieldPath, startOfYear, endOfYear);
                })
                .toList();

        return cb.or(yearPredicates.toArray(new Predicate[0]));
    }

    private Path<?> getFieldPath(Root<?> root, String fieldName) {
        var parts = fieldName.split("\\.");
        Path<?> path = root;

        for (var part : parts) {
            path = path.get(part);
        }

        return path;
    }

    private boolean isNullOperation() {
        return type == OperationType.IS_NULL || type == OperationType.IS_NOT_NULL;
    }

    private List<String> getValues() {
        if (values != null && !values.isEmpty()) {
            return values;
        } else if (singleValue != null && !singleValue.isBlank()) {
            return Collections.singletonList(singleValue);
        }
        return Collections.emptyList();
    }

    private Predicate createEqualsPredicate(CriteriaBuilder cb, Path<?> path, String value, Class<?> fieldType) {
        var convertedValue = convertValue(value, fieldType);
        return cb.equal(path, convertedValue);
    }

    private Predicate createInPredicate(Path<?> path, List<String> values, Class<?> fieldType) {
        var convertedValues = values.stream()
                .map(v -> convertValue(v, fieldType))
                .toList();
        return path.in(convertedValues);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate createComparisonPredicate(CriteriaBuilder cb,
                                                Path<?> path,
                                                String value,
                                                Class<?> fieldType,
                                                ComparisonType comparisonType) {
        if (!Comparable.class.isAssignableFrom(fieldType)) {
            throw new IllegalArgumentException("Field type must be Comparable for comparison operations");
        }

        var comparablePath = (Path<Comparable>) path;
        var convertedValue = (Comparable) convertValue(value, fieldType);

        return switch (comparisonType) {
            case GREATER -> cb.greaterThan(comparablePath, convertedValue);
            case GREATER_OR_EQUAL -> cb.greaterThanOrEqualTo(comparablePath, convertedValue);
            case LESS -> cb.lessThan(comparablePath, convertedValue);
            case LESS_OR_EQUAL -> cb.lessThanOrEqualTo(comparablePath, convertedValue);
        };
    }

    private Object convertValue(String value, Class<?> targetType) {
        // Early return for String type to avoid unnecessary processing
        if (targetType == String.class) {
            return value;
        }

        try {

            // Check for converter in the map
            if (TYPE_CONVERTERS.containsKey(targetType)) {
                return TYPE_CONVERTERS.get(targetType).apply(value);
            }

            // Special case for enum types
            if (Enum.class.isAssignableFrom(targetType)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
                return Enum.valueOf(enumType, value.toUpperCase());
            }

            throw new IllegalArgumentException("Unsupported field type: " + targetType.getName());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Failed to convert value '%s' to type %s", value, targetType.getSimpleName()), e);
        }
    }

    private enum ComparisonType {
        GREATER, GREATER_OR_EQUAL, LESS, LESS_OR_EQUAL
    }
}