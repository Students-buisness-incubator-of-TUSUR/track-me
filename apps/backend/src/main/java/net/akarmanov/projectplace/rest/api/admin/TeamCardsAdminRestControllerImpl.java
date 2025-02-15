package net.akarmanov.projectplace.rest.api.admin;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.spec.TeamCardSpecification;
import net.akarmanov.projectplace.mapping.TeamCardMapper;
import net.akarmanov.projectplace.filters.Filter;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamCardsAdminRestControllerImpl implements TeamCardsAdminRestController {

  private final TeamCardsService teamCardsService;

  private final TeamCardMapper teamCardMapper;

  @Override
  public ResponseEntity<TeamCardDto> createTeamCard(TeamCardCreateOrUpdateDto dto, UUID userId) {
    var teamCard = teamCardMapper.mapToEntity(dto);
    teamCard = teamCardsService.createTeamCard(teamCard, userId);
    var teamCardDto = teamCardMapper.mapToDto(teamCard);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  public ResponseEntity<TeamCardDto> updateTeamCard(UUID teamCardId, UUID userId,
                                                    TeamCardCreateOrUpdateDto updateDto) {
    var teamCard = teamCardMapper.mapToEntity(updateDto);
    teamCard = teamCardsService.updateTeamCard(teamCardId, teamCard, userId);
    var teamCardDto = teamCardMapper.mapToDto(teamCard);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  public ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(Pageable pageable,
                                                              List<Filter> filters) {
    var specs = TeamCardSpecification.withFilters(filters);
    var page = teamCardsService.findAll(specs, pageable)
        .map(teamCardMapper::mapToDto);
    return ResponseEntity.ok(new PagedModel<>(page));
  }

  @Override
  public ResponseEntity<TeamCardDto> getTeamCard(UUID id, UUID userId) {
    var teamCard = teamCardsService.getTeamCard(id, userId);
    return ResponseEntity.ok(teamCardMapper.mapToDto(teamCard));
  }

  @Override
  public ResponseEntity<Void> deleteTeamCard(UUID id, UUID userId) {
    teamCardsService.deleteTeamCard(id, userId);
    return ResponseEntity.noContent().build();
  }
}
