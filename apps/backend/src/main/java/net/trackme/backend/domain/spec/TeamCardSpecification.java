package net.trackme.backend.domain.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.trackme.backend.domain.TeamCard;
import static net.trackme.backend.domain.spec.SpecificationUtils.getReadinessLevelFilter;
import static net.trackme.backend.domain.spec.SpecificationUtils.collectFilterValues;
import static net.trackme.backend.domain.spec.SpecificationUtils.createTeamCardExistsPredicate;
import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.FilterFieldNotAllowedException;

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

    /** Поле для фильтрации по рынкам НТИ (как приходит с фронта). */
    public static final String NTI_MARKETS_NAME_FIELD = "ntiMarkets.name";

    /**
     * Список допустимых полей для фильтрации карточек команд.
     */
    public static final List<String> ALLOWED_FIELDS = List.of(
            "name",
            NTI_MARKETS_NAME_FIELD,
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

        // Разделяем фильтры на обычные и коллекционные
        List<Filter> marketFilters = new ArrayList<>();
        List<Filter> readinessFilters = new ArrayList<>();
        List<Filter> otherFilters = new ArrayList<>();

        for (var filter : filters) {
            if (null == filter.fieldName()) {
                otherFilters.add(filter);
            } else switch (filter.fieldName()) {
                case NTI_MARKETS_NAME_FIELD -> marketFilters.add(filter);
                case READINESS_LEVEL_FIELD_NAME -> 
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
            predicates.add(createTeamCardExistsPredicate(root, query, criteriaBuilder,
                    collectFilterValues(marketFilters), NTI_MARKETS_FIELD, "name"));
        }

        // Обработка фильтра по TRL (OR логика)
        if (!readinessFilters.isEmpty()) {
            predicates.add(createTeamCardExistsPredicate(root, query, criteriaBuilder,
                    collectFilterValues(readinessFilters), null, READINESS_LEVEL_FIELD_NAME));
        }

        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }
}
