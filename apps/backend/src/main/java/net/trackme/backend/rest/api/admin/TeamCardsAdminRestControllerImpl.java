package net.trackme.backend.rest.api.admin;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.trackme.backend.commons.filters.FilterRequest;
import net.trackme.backend.domain.spec.TeamCardSpecification;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.teamcard.TeamCardsService;
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
                                                    String username) {
    var teamCard = teamCardMapper.mapToEntity(dto);
    var ntiMarketIds = dto.ntiMarketIds();
    var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

    teamCard.setNtiMarkets(ntiMarkets);
    teamCard = teamCardsService.createTeamCard(teamCard, streamId, username);
    var teamCardDto = teamCardMapper.mapToDto(teamCard);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  @Transactional
  public ResponseEntity<TeamCardDto> updateTeamCard(UUID teamCardId,
                                                    String username, UUID streamId,
                                                    TeamCardCreateOrUpdateDto updateDto) {
    var teamCard = teamCardMapper.mapToEntity(updateDto);
    var ntiMarketIds = updateDto.ntiMarketIds();
    var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

    teamCard.setNtiMarkets(ntiMarkets);
    teamCard = teamCardsService.updateTeamCard(teamCardId, teamCard, streamId, username);
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
  public ResponseEntity<TeamCardDto> getTeamCard(UUID id, String username) {
    var teamCard = teamCardsService.getTeamCard(id, username);
    return ResponseEntity.ok(teamCardMapper.mapToDto(teamCard));
  }

  @Override
  public ResponseEntity<Void> deleteTeamCard(UUID id, String username) {
    teamCardsService.deleteTeamCard(id, username);
    return ResponseEntity.noContent().build();
  }
}
