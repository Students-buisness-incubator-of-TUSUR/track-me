package net.akarmanov.projectplace.services.teamcard;

import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public interface TeamCardsService {
  TeamCard createTeamCard(TeamCard createTeamCardDto);

  TeamCard updateTeamCard(UUID teamCardId, TeamCard updateTeamCardDto);

  Page<TeamCard> getTeamCards(Specification<TeamCard> specification, Pageable pageable, UUID userId);

  TeamCard getTeamCard(UUID id);

  void deleteTeamCard(UUID id);

  TeamCard createTeamCard(TeamCard teamCard, UUID userId);

  TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCard, UUID userId);

  Page<TeamCard> findAll(Specification<TeamCard> specification, Pageable pageable);

  TeamCard getTeamCard(UUID id, UUID userId);

  void deleteTeamCard(UUID id, UUID userId);
}
