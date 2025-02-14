package net.akarmanov.projectplace.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.filters.Filter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamCardSpecification implements Specification<TeamCard> {

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

  public static Specification<TeamCard> userEquals(UUID userId) {
    return (root, query, criteriaBuilder) -> {
      var userJoin = root.join("user");
      return criteriaBuilder.equal(userJoin.get("id"), userId);
    };
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
      predicates.add(filter.toPredicate(root, criteriaBuilder));
    }
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
