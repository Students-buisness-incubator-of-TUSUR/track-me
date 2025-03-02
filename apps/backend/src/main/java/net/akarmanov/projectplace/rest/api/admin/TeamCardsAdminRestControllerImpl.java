package net.akarmanov.projectplace.rest.api.admin;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.spec.TeamCardSpecification;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.mapping.TeamCardMapper;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import net.akarmanov.projectplace.services.nti.NtiMarketService;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamCardsAdminRestControllerImpl implements TeamCardsAdminRestController {

  private final TeamCardsService teamCardsService;

  private final TeamCardMapper teamCardMapper;

  private final NtiMarketService ntiMarketService;

  @Override
  @Transactional
  public ResponseEntity<TeamCardDto> createTeamCard(TeamCardCreateOrUpdateDto dto,
                                                    UUID streamId,
                                                    UUID userId) {
    var teamCard = teamCardMapper.mapToEntity(dto);
    var ntiMarketId = dto.ntiMarketId();

    teamCard.setNtiMarket(ntiMarketService.getNtiMarket(ntiMarketId));
    teamCard = teamCardsService.createTeamCard(teamCard, streamId, userId);
    var teamCardDto = teamCardMapper.mapToDto(teamCard);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  @Transactional
  public ResponseEntity<TeamCardDto> updateTeamCard(UUID teamCardId,
                                                    UUID userId,
                                                    UUID streamId,
                                                    TeamCardCreateOrUpdateDto updateDto) {
    var teamCard = teamCardMapper.mapToEntity(updateDto);
    var ntiMarketId = updateDto.ntiMarketId();

    teamCard.setNtiMarket(ntiMarketService.getNtiMarket(ntiMarketId));
    teamCard = teamCardsService.updateTeamCard(teamCardId, teamCard, streamId, userId);
    var teamCardDto = teamCardMapper.mapToDto(teamCard);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  public ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(Pageable pageable,
                                                              FilterRequest filterRequest) {
    var specs = TeamCardSpecification.withFilters(filterRequest.filters());
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
