package net.trackme.backend.domain.spec;

import net.trackme.backend.commons.filters.Filter;
import net.trackme.backend.domain.ReadinessLevel;

public class SpecificationUtils {

  private SpecificationUtils() {
    // Utility class
  }

  public static Filter getStartDateFilter(Filter filter) {
    return Filter.builder()
        .fieldName("startDate")
        .type(filter.type())
        .singleValue(filter.singleValue())
        .values(filter.values())
        .build();
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
}
