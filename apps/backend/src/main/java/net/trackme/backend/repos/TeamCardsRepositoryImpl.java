package net.trackme.backend.repos;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.trackme.backend.domain.TeamCard;
import org.springframework.data.jpa.domain.Specification;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamCardsRepositoryImpl implements TeamCardsRepositoryCustom {
    private static final String USERNAME_FIELD = "username";

    @PersistenceContext
    private EntityManager entityManager;

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