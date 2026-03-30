package net.trackme.meetingservice.entities;


import jakarta.persistence.criteria.*;
import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.FilterFieldNotAllowedException;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MeetingSpecification(List<Filter> filters) implements Specification<Meeting> {

    private static final String TEAM_NAME_FIELD = "teamName";
    private static final String TRACKER_USERNAME_FIELD = "trackerUsername";
    private static final String TRACKER_FULL_NAME = "trackerFullName";
    private static final String STREAM_IDS_FIELD = "streamIds";
    private static final String STATUS_FIELD = "status";
    private static final String START_DATE_FIELD = "startDate";
    private static final String TEAM_STATUS_VALUE_FIELD = "teamStatusValue";

    public static final List<String> ALLOWED_FIELDS = List.of(
            TEAM_NAME_FIELD,
            TRACKER_FULL_NAME,
            TRACKER_USERNAME_FIELD,
            STREAM_IDS_FIELD,
            STATUS_FIELD,
            START_DATE_FIELD,
            TEAM_STATUS_VALUE_FIELD,
            "teamStatus"
    );

    /**
     * Создаёт спецификацию на основе списка фильтров.
     */
    public static MeetingSpecification withFilters(List<Filter> filters) {
        return new MeetingSpecification(filters);
    }

    /**
     * Спецификация для фильтрации по конкретному потоку.
     * Используется как обязательный фильтр в методах отчёта.
     */
    public static Specification<Meeting> belongsToStream(UUID streamId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.isMember(streamId, root.get(STREAM_IDS_FIELD));
        };
    }

    /**
     * Спецификация для загрузки связанных идентификаторов потоков.
     */
    public static Specification<Meeting> withFetchJoins() {
        return (root, query, cb) -> {
            if (query != null && Meeting.class.equals(query.getResultType())) {
                query.distinct(true);
                root.fetch(STREAM_IDS_FIELD, JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    /**
     * Создаёт спецификацию на основе вхождения в список идентификаторов.
     */
    public static Specification<Meeting> idIn(List<UUID> ids) {
        return (root, query, cb) -> ids.isEmpty()
                ? cb.disjunction()
                : root.get("id").in(ids);
    }

    public static Specification<Meeting> teamCardIdEquals(UUID teamCardId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("teamCardId"), teamCardId);
    }

    public static Specification<Meeting> meetingIdEquals(UUID meetingId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), meetingId);
    }

    @Override
    public Predicate toPredicate(
            Root<Meeting> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder
    ) {
        if (query != null && !Long.class.equals(query.getResultType())) {
            query.distinct(true);
        }

        List<Predicate> predicates = new ArrayList<>();
        for (var filter : filters) {
            if (!ALLOWED_FIELDS.contains(filter.fieldName())) {
                throw new FilterFieldNotAllowedException(filter.fieldName(), ALLOWED_FIELDS);
            }
            predicates.add(filter.toPredicate(root, criteriaBuilder));
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
