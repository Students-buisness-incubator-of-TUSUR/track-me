package net.trackme.backend.domain.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import static net.trackme.backend.domain.spec.SpecificationUtils.getReadinessLevelFilter;
import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.FilterFieldNotAllowedException;

public class StreamSpecification implements Specification<Stream> {

    public static final String READINESS_LEVEL_FIELD = "teamCards.readinessLevel";
    public static final String NTIMARKETS_NAME_FIELD = "ntiMarkets.name";

    public static final List<String> ALLOWED_FIELDS = List.of(
            "name",
            "active",
            "year",
            NTIMARKETS_NAME_FIELD,
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

        // ВАЖНО: добавляем DISTINCT для устранения дублирования
        query.distinct(true);

        List<Predicate> predicates = new ArrayList<>();

        // Разделяем фильтры на обычные и коллекционные
        List<Filter> marketFilters = new ArrayList<>();
        List<Filter> readinessFilters = new ArrayList<>();
        List<Filter> otherFilters = new ArrayList<>();

        for (var filter : filters) {
            if (null == filter.fieldName()) {
                otherFilters.add(filter);
            } else switch (filter.fieldName()) {
                case NTIMARKETS_NAME_FIELD -> marketFilters.add(filter);
                case READINESS_LEVEL_FIELD -> 
                    readinessFilters.add(getReadinessLevelFilter(filter));
                default -> otherFilters.add(filter);
            }
        }

        // Обработка обычных фильтров (без JOIN)
        for (var filter : otherFilters) {
            if (!ALLOWED_FIELDS.contains(filter.fieldName())) {
                throw new FilterFieldNotAllowedException(filter.fieldName(), ALLOWED_FIELDS);
            }
            predicates.add(filter.toPredicate(root, criteriaBuilder));
        }

        // Обработка фильтра по рынкам НТИ (OR логика)
        if (!marketFilters.isEmpty()) {
            predicates.add(createMarketsOrPredicate(root, query, criteriaBuilder, marketFilters));
        }

        // Обработка фильтра по TRL (OR логика)
        if (!readinessFilters.isEmpty()) {
            predicates.add(createReadinessOrPredicate(root, query, criteriaBuilder, readinessFilters));
        }

        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    /**
     * OR логика для рынков: поток должен содержать ЛЮБОЙ из выбранных рынков
     */
    private Predicate createMarketsOrPredicate(
            Root<Stream> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            List<Filter> marketFilters) {

        // Собираем все значения из фильтров по рынкам
        List<String> marketNames = new ArrayList<>();
        for (Filter filter : marketFilters) {
            List<String> filterValues = getFilterValues(filter);
            marketNames.addAll(filterValues);
        }

        if (marketNames.isEmpty()) {
            return cb.conjunction();
        }

        // Создаем подзапрос для OR логики
        Subquery<Stream> subquery = query.subquery(Stream.class);
        Root<Stream> subStream = subquery.from(Stream.class);
        Join<Stream, NTIMarket> subMarkets = subStream.join("ntiMarkets");

        subquery.select(subStream)
                .where(
                        cb.equal(subStream.get("id"), root.get("id")),
                        subMarkets.get("name").in(marketNames)  // ЛЮБОЙ из списка
                );

        return cb.exists(subquery);
    }

    /**
     * OR логика для TRL: поток должен содержать ЛЮБОЙ из выбранных уровней TRL
     */
    private Predicate createReadinessOrPredicate(
            Root<Stream> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            List<Filter> readinessFilters) {

        // Собираем все значения из фильтров по TRL
        List<String> readinessLevels = new ArrayList<>();
        for (Filter filter : readinessFilters) {
            List<String> filterValues = getFilterValues(filter);
            readinessLevels.addAll(filterValues);
        }

        if (readinessLevels.isEmpty()) {
            return cb.conjunction();
        }

        // Создаем подзапрос для OR логики
        Subquery<Stream> subquery = query.subquery(Stream.class);
        Root<Stream> subStream = subquery.from(Stream.class);
        Join<Stream, TeamCard> subTeamCards = subStream.join("teamCards");

        subquery.select(subStream)
                .where(
                        cb.equal(subStream.get("id"), root.get("id")),
                        subTeamCards.get("readinessLevel").in(readinessLevels)  // ЛЮБОЙ из списка
                );

        return cb.exists(subquery);
    }

    /**
     * Извлекает значения из фильтра (из values или singleValue)
     */
    private List<String> getFilterValues(Filter filter) {
        if (filter.values() != null && !filter.values().isEmpty()) {
            return filter.values();
        } else if (filter.singleValue() != null && !filter.singleValue().isBlank()) {
            return List.of(filter.singleValue());
        }
        return List.of();
    }
}