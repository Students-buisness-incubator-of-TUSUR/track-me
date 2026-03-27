package net.trackme.backend.repos;

import net.trackme.backend.domain.TeamCard;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public interface TeamCardsRepositoryCustom {

    Map<String, Double> getAverageGradesByUser(Specification<TeamCard> spec);
}
