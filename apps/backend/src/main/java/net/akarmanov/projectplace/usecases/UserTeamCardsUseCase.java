package net.akarmanov.projectplace.usecases;

import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserTeamCardsUseCase {
  TeamCardDto createTeamCard(TeamCardCreateOrUpdateDto teamCard);

  TeamCardDto updateTeamCard(UUID teamCardId, TeamCardCreateOrUpdateDto createOrUpdateDto);

  Page<TeamCardDto> getTeamCards(String name, String status, Pageable pageable);

  TeamCardDto getTeamCard(UUID id);

  void deleteTeamCard(UUID id);
}
