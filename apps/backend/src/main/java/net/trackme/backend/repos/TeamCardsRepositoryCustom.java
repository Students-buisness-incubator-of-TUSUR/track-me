package net.trackme.backend.repos;

import net.trackme.backend.domain.TeamCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public interface TeamCardsRepositoryCustom {

    Map<String, Double> getAverageGradesByUser(Specification<TeamCard> spec);

    Page<TeamCard> findAllByStreamOrder(Specification<TeamCard> spec, Pageable pageable);
}
