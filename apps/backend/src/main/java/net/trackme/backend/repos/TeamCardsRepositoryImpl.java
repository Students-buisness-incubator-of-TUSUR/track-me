package net.trackme.backend.repos;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.domain.spec.TeamCardOrdering;
import net.trackme.backend.domain.spec.TeamCardSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamCardsRepositoryImpl implements TeamCardsRepositoryCustom {
    private static final String USERNAME_FIELD = "username";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<TeamCard> findAllByStreamOrder(Specification<TeamCard> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<TeamCard> root = query.from(TeamCard.class);
        var filters = spec == null ? TeamCardSpecification.hasStream()
                : spec.and(TeamCardSpecification.hasStream());
        Predicate predicate = filters.toPredicate(root, query, cb);
        var ordering = TeamCardOrdering.byStreamEndDate(root, query, cb);
        List<Selection<?>> selections = new ArrayList<>();
        selections.add(root.alias("teamCard"));
        selections.addAll(ordering.keys());
        query.multiselect(selections).distinct(true)
                .where(predicate, ordering.selectedStream()).orderBy(ordering.orders());

        var typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
            typedQuery.setMaxResults(pageable.getPageSize());
        }
        var cards = typedQuery.getResultList().stream()
                .map(tuple -> tuple.get("teamCard", TeamCard.class)).toList();
        return PageableExecutionUtils.getPage(cards, pageable, () -> {
            CriteriaQuery<Long> count = cb.createQuery(Long.class);
            Root<TeamCard> countRoot = count.from(TeamCard.class);
            count.select(cb.countDistinct(countRoot)).where(filters.toPredicate(countRoot, count, cb));
            return entityManager.createQuery(count).getSingleResult();
        });
    }

    @Override
    public Map<String, Double> getAverageGradesByUser(Specification<TeamCard> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<TeamCard> root = query.from(TeamCard.class);

        query.multiselect(
                root.get(USERNAME_FIELD).alias(USERNAME_FIELD),
                cb.avg(root.get("averageGrade")).alias("avgGrade")
        );

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        query.groupBy(root.get(USERNAME_FIELD));

        return entityManager.createQuery(query)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(USERNAME_FIELD, String.class),
                        tuple -> {
                            Double avg = tuple.get("avgGrade", Double.class);
                            return avg != null ? avg : 0.0;
                        }
                ));
    }
}
