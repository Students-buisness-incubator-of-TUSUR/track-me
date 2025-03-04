package net.akarmanov.projectplace.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.akarmanov.projectplace.domain.ReadinessLevel;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.filters.Filter;
import net.akarmanov.projectplace.filters.FilterFieldNotAllowedException;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StreamSpecification implements Specification<Stream> {

  public static final String READINESS_LEVEL_FIELD = "teamCards.readinessLevel";

  public static final String YEAR_FIELD = "year";

  public static final List<String> ALLOWED_FIELDS = List.of(
      "name",
      YEAR_FIELD,
      "ntiMarkets.name",
      READINESS_LEVEL_FIELD
  );

  private final transient List<Filter> filters;

  private StreamSpecification(List<Filter> filters) {
    this.filters = filters;
  }

  public static Specification<Stream> withFilters(List<Filter> filters) {
    return new StreamSpecification(filters);
  }

  static Filter getReadinessLevelFilter(Filter filter) {
    var readinessLevel = ReadinessLevel.fromValue(filter.singleValue());
    return Filter.builder()
        .fieldName(filter.fieldName())
        .type(filter.type())
        .singleValue(readinessLevel == null ? null : readinessLevel.name())
        .values(filter.values() != null
            ? filter.values().stream()
            .map(ReadinessLevel::fromValue)
            .map(ReadinessLevel::name)
            .toList()
            : null)
        .build();
  }

  @Override
  public Predicate toPredicate(Root<Stream> root,
                               CriteriaQuery<?> query,
                               CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = new ArrayList<>();
    for (var filter : filters) {
      if (!ALLOWED_FIELDS.contains(filter.fieldName())) {
        throw new FilterFieldNotAllowedException(filter.fieldName(), ALLOWED_FIELDS);
      }
      if (READINESS_LEVEL_FIELD.equals(filter.fieldName())) {
        filter = getReadinessLevelFilter(filter);
      }
      if (YEAR_FIELD.equals(filter.fieldName())) {
        filter = getStartDateFilter(filter);
      }
      predicates.add(filter.toPredicate(root, criteriaBuilder));
    }
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }

  private Filter getStartDateFilter(Filter filter) {
    return Filter.builder()
        .fieldName("startDate")
        .type(filter.type())
        .singleValue(filter.singleValue())
        .values(filter.values())
        .build();
  }
}
