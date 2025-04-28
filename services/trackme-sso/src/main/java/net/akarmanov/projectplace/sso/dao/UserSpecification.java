package net.akarmanov.projectplace.sso.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.akarmanov.projectplace.commons.filters.Filter;
import net.akarmanov.projectplace.commons.filters.FilterFieldNotAllowedException;
import net.akarmanov.projectplace.sso.dao.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class UserSpecification implements Specification<UserEntity> {

  private static final List<String> ALLOWED_FIELDS =
      List.of("fullName", "username");


  private final transient List<Filter> filters;

  public UserSpecification(List<Filter> filters) {
    this.filters = filters;
  }

  public static Specification<UserEntity> byRole(String role) {
    return (root, query, criteriaBuilder) -> {
      var roleJoin = root.join("roles");
      return criteriaBuilder.equal(roleJoin.get("code"), role);
    };
  }

  public static Specification<UserEntity> withFilters(List<Filter> filters) {
    return new UserSpecification(filters);
  }

  @Override
  public Predicate toPredicate(Root<UserEntity> root,
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
