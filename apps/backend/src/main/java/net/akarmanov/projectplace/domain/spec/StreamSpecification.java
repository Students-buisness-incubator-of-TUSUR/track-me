package net.akarmanov.projectplace.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.filters.Filter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StreamSpecification implements Specification<Stream> {

  private final transient List<Filter> filters;

  private StreamSpecification(List<Filter> filters) {
    this.filters = filters;
  }

  public static StreamSpecification withFilters(List<Filter> filters) {
    return new StreamSpecification(filters);
  }

  @Override
  public Predicate toPredicate(Root<Stream> root,
                               CriteriaQuery<?> query,
                               CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = new ArrayList<>();
    for (var filter : filters) {
      predicates.add(filter.toPredicate(root, criteriaBuilder));
    }
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
