package net.akarmanov.projectplace.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.filters.Filter;
import net.akarmanov.projectplace.filters.FilterFieldNotAllowedException;
import net.akarmanov.projectplace.models.UserRole;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class UserSpecification implements Specification<User> {

  private static final List<String> ALLOWED_FIELDS =
      List.of("fullName", "telegramId");


  private final transient List<Filter> filters;

  public UserSpecification(List<Filter> filters) {
    this.filters = filters;
  }

  public static Specification<User> byRole(UserRole role) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("role"), role.name());
  }

  public static Specification<User> undeleted() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
  }

  public static Specification<User> withFilters(List<Filter> filters) {
    return new UserSpecification(filters)
        .and(undeleted());
  }

  @Override
  public Predicate toPredicate(Root<User> root,
                               CriteriaQuery<?> query,
                               CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = filters.stream()
        .map(filter -> {
          if (!ALLOWED_FIELDS.contains(filter.fieldName())) {
            throw new FilterFieldNotAllowedException(filter.fieldName());
          }
          return filter.toPredicate(root, criteriaBuilder);
        })
        .toList();
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
