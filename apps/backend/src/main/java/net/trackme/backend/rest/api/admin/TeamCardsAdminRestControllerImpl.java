package net.trackme.backend.rest.api.admin;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.domain.spec.TeamCardSpecification;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.teamcard.TeamCardsService;
import net.trackme.commons.filters.FilterRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TeamCardsAdminRestControllerImpl implements TeamCardsAdminRestController {

  private final TeamCardsService teamCardsService;

  private final TeamCardMapper teamCardMapper;

  private final NtiMarketService ntiMarketService;

  private static final Logger log = LoggerFactory.getLogger(TeamCardsAdminRestControllerImpl.class);

  @Override
  @Transactional
  public ResponseEntity<TeamCardDto> createTeamCard(TeamCardCreateDto dto,
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
                                                    TeamCardUpdateDto updateDto) {
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

  @Override
  public ResponseEntity<List<Map<String, String>>> getTeamsByUser(String username) {
    log.info("Getting teams for user: {}", username);
    List<TeamCard> teams = teamCardsService.getTeamCardsByUser(username);
    
    List<Map<String, String>> result = teams.stream()
        .map(team -> Map.of(
            "id", team.getId().toString(),
            "name", team.getName()
        ))
        .toList();
    
    return ResponseEntity.ok(result);
  }

  @Override
  @Transactional
  public ResponseEntity<Void> reassignTeams(TeamCardsAdminRestController.ReassignTeamsRequest request) {
    String fromUsername = request.fromUsername().trim();
    String toUsername = request.toUsername().trim();
    String toUserFullName = (request.toUserFullName() != null) 
        ? request.toUserFullName().trim() 
        : null;
    
    // Проверка на совпадение usernames
    if (fromUsername.equals(toUsername)) {
      log.warn("Attempted reassign with identical usernames: {}", fromUsername);
      return ResponseEntity.badRequest().build();
    }
    
    log.info("Reassigning all teams from '{}' to '{}' with fullName: '{}'", 
        fromUsername, toUsername, toUserFullName);
    
    teamCardsService.reassignTeams(fromUsername, toUsername, toUserFullName);
    
    log.info("Successfully reassigned teams from '{}' to '{}'", fromUsername, toUsername);
    return ResponseEntity.ok().build();
  }
}
