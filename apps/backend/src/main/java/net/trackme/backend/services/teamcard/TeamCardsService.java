package net.trackme.backend.services.teamcard;

import net.trackme.backend.domain.TeamCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface TeamCardsService {
    TeamCard createTeamCard(TeamCard createTeamCardDto);

    TeamCard updateTeamCard(UUID teamCardId, TeamCard updateTeamCardDto);

    Page<TeamCard> getTeamCards(Specification<TeamCard> specification, Pageable pageable);

    TeamCard getTeamCard(UUID id);

    void deleteTeamCard(UUID id);

    TeamCard createTeamCard(TeamCard teamCard, String username);

    TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCard, UUID streamId, String username);

    Page<TeamCard> findAll(Specification<TeamCard> specification, Pageable pageable);

    TeamCard getTeamCard(UUID id, String username);

    void deleteTeamCard(UUID id, String username);

    TeamCard createTeamCard(TeamCard teamCard, UUID streamId, String username);

    Integer getTeamCardCount(UUID streamId);
}
