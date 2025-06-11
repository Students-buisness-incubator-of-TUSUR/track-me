package net.trackme.backend.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.trackme.backend.domain.TeamCard;
import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.FilterFieldNotAllowedException;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static net.trackme.backend.domain.spec.SpecificationUtils.getReadinessLevelFilter;

public class TeamCardSpecification implements Specification<TeamCard> {

    public static final String READINESS_LEVEL_FIELD_NAME = "readinessLevel";

    public static final List<String> ALLOWED_FIELDS = List.of(
            "name",
            "ntiMarkets.name",
            "description",
            "status",
            "username",
            READINESS_LEVEL_FIELD_NAME,
            "streams.year",
            "streams.name",
            "enabled"
    );

    private final transient List<Filter> filters;

    private TeamCardSpecification(List<Filter> filters) {
        this.filters = filters;
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
            if (READINESS_LEVEL_FIELD_NAME.equals(filter.fieldName())) {
                filter = getReadinessLevelFilter(filter);
            }
            predicates.add(filter.toPredicate(root, criteriaBuilder));
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
