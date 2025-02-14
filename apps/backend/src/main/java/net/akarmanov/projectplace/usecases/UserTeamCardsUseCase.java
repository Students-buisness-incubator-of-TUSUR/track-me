package net.akarmanov.projectplace.usecases;

import net.akarmanov.projectplace.models.Filter;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserTeamCardsUseCase {
  TeamCardDto createTeamCard(TeamCardCreateOrUpdateDto teamCard);

  TeamCardDto updateTeamCard(UUID teamCardId, TeamCardCreateOrUpdateDto createOrUpdateDto);

  Page<TeamCardDto> getTeamCards(List<Filter> filters, Pageable pageable);

  TeamCardDto getTeamCard(UUID id);

  void deleteTeamCard(UUID id);
}
