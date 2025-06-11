package net.trackme.backend.domain.spec;

import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.commons.filters.Filter;

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
}
