package net.akarmanov.projectplace.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.akarmanov.projectplace.commons.filters.Filter;
import net.akarmanov.projectplace.commons.filters.FilterFieldNotAllowedException;
import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static net.akarmanov.projectplace.domain.spec.SpecificationUtils.getReadinessLevelFilter;

public class TeamCardSpecification implements Specification<TeamCard> {

  public static final String READINESS_LEVEL_FIELD_NAME = "readinessLevel";

  public static final String STREAMS_YEAR_FIELD = "streams.year";

  public static final List<String> ALLOWED_FIELDS = List.of(
      "name",
      "ntiMarket.name",
      "description",
      "status",
      "username",
      READINESS_LEVEL_FIELD_NAME,
      STREAMS_YEAR_FIELD,
      "streams.name"
  );

  private final transient List<Filter> filters;

  private TeamCardSpecification(List<Filter> filters) {
    this.filters = filters;
  }

  public static Specification<TeamCard> nameLike(String name) {
    return (root, query, builder) -> {
      if (name == null) {
        return builder.conjunction();
      }
      return builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    };
  }

  public static Specification<TeamCard> statusEquals(String status) {
    return (root, query, criteriaBuilder) -> {
      if (status == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("status"), status);
    };
  }

  public static Specification<TeamCard> userEquals(String username) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("username"), username);
  }

  public static TeamCardSpecification withFilters(List<Filter> filters) {
    return new TeamCardSpecification(filters);
  }

  @Override
  public Predicate toPredicate(Root<TeamCard> root,
                               CriteriaQuery<?> query,
                               CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = new ArrayList<>();
    for (var filter : filters) {
      if (!ALLOWED_FIELDS.contains(filter.fieldName())) {
        throw new FilterFieldNotAllowedException(filter.fieldName(), ALLOWED_FIELDS);
      }

      if (STREAMS_YEAR_FIELD.equals(filter.fieldName())) {
        filter = Filter.builder()
            .fieldName("streams.startDate")
            .type(filter.type())
            .values(filter.values())
            .singleValue(filter.singleValue())
            .build();
      }

      if (READINESS_LEVEL_FIELD_NAME.equals(filter.fieldName())) {
        filter = getReadinessLevelFilter(filter);
      }
      predicates.add(filter.toPredicate(root, criteriaBuilder));
    }
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
