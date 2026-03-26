package net.trackme.backend.domain.spec;

import jakarta.persistence.criteria.*;
import net.trackme.backend.domain.TeamCard;
import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.FilterFieldNotAllowedException;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.trackme.backend.domain.spec.SpecificationUtils.getReadinessLevelFilter;

/**
 * Спецификация для фильтрации карточек команд по заданным критериям.
 */
public class TeamCardSpecification implements Specification<TeamCard> {

    /** Поле имени пользователя. */
    private static final String USERNAME_FIELD = "username";

    /** Поле потоков. */
    private static final String STREAMS_FIELD = "streams";

    /** Поле рынков НТИ. */
    private static final String NTI_MARKETS_FIELD = "ntiMarkets";

    /** Поле уровня готовности. */
    public static final String READINESS_LEVEL_FIELD_NAME = "readinessLevel";

    /**
     * Список допустимых полей для фильтрации карточек команд.
     */
    public static final List<String> ALLOWED_FIELDS = List.of(
            "name",
            "ntiMarkets.name",
            "description",
            "status",
            USERNAME_FIELD,
            READINESS_LEVEL_FIELD_NAME,
            "streams.year",
            "streams.name",
            "enabled",
            "averageGrade",
            "streams.startDate",
            "streams.endDate"
    );

    private final transient List<Filter> filters;

    private TeamCardSpecification(List<Filter> filters) {
        this.filters = filters;
    }

    /**
     * Спецификация для фильтрации карточек команд по имени пользователя.
     *
     * @param username имя пользователя
     * @return спецификация
     */
    public static Specification<TeamCard> userEquals(String username) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(USERNAME_FIELD), username);
    }

    /**
     * Спецификация для фильтрации карточек команд,
     * у которых есть хотя бы один привязанный поток.
     *
     * @return спецификация
     */
    public static Specification<TeamCard> hasStream() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNotEmpty(root.get(STREAMS_FIELD));
    }

    /**
     * Спецификация для загрузки связанных сущностей потоков и рынков НТИ
     * через LEFT JOIN FETCH во избежание проблемы N+1.
     * Не применяет дополнительной фильтрации.
     *
     * @return спецификация
     */
    public static Specification<TeamCard> withFetchJoins() {
        return (root, query, criteriaBuilder) -> {
            if (query != null && TeamCard.class.equals(query.getResultType())) {
                query.distinct(true);

                boolean alreadyFetchedStreams = root.getFetches().stream()
                        .anyMatch(f -> f.getAttribute().getName().equals(STREAMS_FIELD));
                boolean alreadyFetchedNti = root.getFetches().stream()
                        .anyMatch(f -> f.getAttribute().getName().equals(NTI_MARKETS_FIELD));

                if (!alreadyFetchedStreams) {
                    root.fetch(STREAMS_FIELD, JoinType.LEFT);
                }
                if (!alreadyFetchedNti) {
                    root.fetch(NTI_MARKETS_FIELD, JoinType.LEFT);
                }
            }
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Создаёт спецификацию на основе вхождения в список идентификаторов.
     *
     * @param ids список идентификаторов
     * @return спецификация
     */
    public static Specification<TeamCard> idIn(List<UUID> ids) {
        return (root, query, cb) -> ids.isEmpty()
                ? cb.disjunction()
                : root.get("id").in(ids);
    }

    /**
     * Создаёт спецификацию на основе списка фильтров.
     *
     * @param filters список фильтров
     * @return спецификация
     */
    public static TeamCardSpecification withFilters(List<Filter> filters) {
        return new TeamCardSpecification(filters);
    }

    /**
     * Формирует предикат на основе переданных фильтров.
     * Выбрасывает {@link FilterFieldNotAllowedException} если поле фильтра
     * не входит в список допустимых полей {@link #ALLOWED_FIELDS}.
     *
     * @param root            корневой объект запроса
     * @param query           объект критериального запроса
     * @param criteriaBuilder построитель критериев
     * @return предикат
     */
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
