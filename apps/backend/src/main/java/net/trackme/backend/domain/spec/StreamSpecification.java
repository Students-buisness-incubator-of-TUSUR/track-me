package net.trackme.backend.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.trackme.backend.domain.Stream;
import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.FilterFieldNotAllowedException;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.trackme.backend.domain.spec.SpecificationUtils.getReadinessLevelFilter;

public class StreamSpecification implements Specification<Stream> {

  public static final String READINESS_LEVEL_FIELD = "teamCards.readinessLevel";

  public static final List<String> ALLOWED_FIELDS = List.of(
      "name",
      "active",
      "year",
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

  public static Specification<Stream> currentlyActive() {
    return (root, query, builder) -> {
      var today = LocalDate.now();
      return builder.and(
              builder.or(
                      builder.isNull(root.get("startDate")),
                      builder.lessThanOrEqualTo(root.get("startDate"), today)
              ),
              builder.or(
                      builder.isNull(root.get("endDate")),
                      builder.greaterThanOrEqualTo(root.get("endDate"), today)
              )
      );
    };
  }


  public static Specification<Stream> byId(UUID id) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), id);
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
      predicates.add(filter.toPredicate(root, criteriaBuilder));
    }
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
