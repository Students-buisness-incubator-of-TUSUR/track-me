package net.trackme.backend.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.commons.filters.Filter;

import java.util.ArrayList;
import java.util.List;

public class SpecificationUtils {

  private SpecificationUtils() {
  }

  public static Filter getReadinessLevelFilter(Filter filter) {
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

  /**
   * Извлекает значения из фильтра (из values или singleValue).
   */
  public static List<String> getFilterValues(Filter filter) {
    if (filter.values() != null && !filter.values().isEmpty()) {
      return filter.values();
    } else if (filter.singleValue() != null && !filter.singleValue().isBlank()) {
      return List.of(filter.singleValue());
    }
    return List.of();
  }

  /**
   * Собирает все значения из списка фильтров.
   */
  public static List<String> collectFilterValues(List<Filter> filters) {
    List<String> values = new ArrayList<>();
    for (Filter filter : filters) {
      values.addAll(getFilterValues(filter));
    }
    return values;
  }

  /**
   * Создаёт OR-предикат с EXISTS-подзапросом для TeamCard.
   */
  public static Predicate createTeamCardExistsPredicate(
      Root<TeamCard> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      List<String> filterValues,
      String joinField,
      String joinTargetField) {

    if (filterValues.isEmpty()) {
      return cb.conjunction();
    }

    Subquery<TeamCard> subquery = query.subquery(TeamCard.class);
    Root<TeamCard> subRoot = subquery.from(TeamCard.class);

    if (joinField != null) {
      Join<TeamCard, NTIMarket> join = subRoot.join(joinField);
      subquery.select(subRoot)
          .where(
              cb.equal(subRoot.get("id"), root.get("id")),
              join.get(joinTargetField).in(filterValues)
          );
    } else {
      subquery.select(subRoot)
          .where(
              cb.equal(subRoot.get("id"), root.get("id")),
              subRoot.get(joinTargetField).in(filterValues)
          );
    }

    return cb.exists(subquery);
  }

  /**
   * Создаёт OR-предикат с EXISTS-подзапросом для Stream.
   */
  public static Predicate createStreamExistsPredicate(
      Root<Stream> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      List<String> filterValues,
      String joinField,
      String joinTargetField) {

    if (filterValues.isEmpty()) {
      return cb.conjunction();
    }

    Subquery<Stream> subquery = query.subquery(Stream.class);
    Root<Stream> subRoot = subquery.from(Stream.class);

    if ("ntiMarkets".equals(joinField)) {
      Join<Stream, NTIMarket> join = subRoot.join(joinField);
      subquery.select(subRoot)
          .where(
              cb.equal(subRoot.get("id"), root.get("id")),
              join.get(joinTargetField).in(filterValues)
          );
    } else if ("teamCards".equals(joinField)) {
      Join<Stream, TeamCard> join = subRoot.join(joinField);
      subquery.select(subRoot)
          .where(
              cb.equal(subRoot.get("id"), root.get("id")),
              join.get(joinTargetField).in(filterValues)
          );
    }

    return cb.exists(subquery);
  }
}
