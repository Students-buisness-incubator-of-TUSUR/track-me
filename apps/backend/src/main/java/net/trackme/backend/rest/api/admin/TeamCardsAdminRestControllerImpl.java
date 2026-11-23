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
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.backend.services.stream.StreamService;
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

  private final StreamService streamService;

  private final OrganizationContext organizationContext;

  private static final Logger log = LoggerFactory.getLogger(TeamCardsAdminRestControllerImpl.class);

  @Override
  @Transactional
  public ResponseEntity<TeamCardDto> createTeamCard(TeamCardCreateDto dto,
                                                    UUID streamId,
                                                    String username) {
    if (streamId != null) {
      organizationContext.checkOrganizationAccess(
          streamService.getById(streamId).getOrganizationId());
    }
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
    var existingTeamCard = teamCardsService.getTeamCard(teamCardId);
    organizationContext.checkOrganizationAccess(existingTeamCard.getOrganizationId());
    if (streamId != null) {
      organizationContext.checkOrganizationAccess(
          streamService.getById(streamId).getOrganizationId());
    }
    var teamCard = teamCardMapper.mapToEntity(updateDto);
    var ntiMarketIds = updateDto.ntiMarketIds();
    var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

    teamCard.setNtiMarkets(ntiMarkets);
    teamCard = teamCardsService.updateTeamCard(teamCardId, teamCard, streamId, username);
    var teamCardDto = teamCardMapper.mapToDto(teamCard);
    return ResponseEntity.ok(teamCardDto);
  }

  /**
   * Изменение только пассивного статуса команды.
   * Не требует заполнения остальных полей карточки.
   * Работает как отдельная операция "Отчислить" / "Вернуть".
   *
   * <p>Использует admin-версию {@code updateTeamCard(UUID, TeamCard, UUID, String)},
   * у которой {@code @PreAuthorize("hasRole('ADMIN')")} — чтобы ADMIN без ACL-записи
   * на команду всё равно мог изменить passive-статус.
   */
  @Override
  @Transactional
  public ResponseEntity<TeamCardDto> updatePassiveStatus(UUID teamCardId,
                                                          PassiveStatusRequest request) {
    log.info("Updating passive status for team card {} to {}",
        teamCardId, request.passive());

    var teamCard = teamCardsService.getTeamCard(teamCardId);
    organizationContext.checkOrganizationAccess(teamCard.getOrganizationId());
    teamCard.setPassive(request.passive());
    var updated = teamCardsService.updateTeamCard(teamCardId, teamCard, null, null);
    var teamCardDto = teamCardMapper.mapToDto(updated);

    log.info("Passive status for team card {} successfully updated to {}",
        teamCardId, teamCardDto.passive());

    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  public ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(Pageable pageable,
                                                              FilterRequest filterRequest) {
    var specs = TeamCardSpecification.withFilters(filterRequest.filters())
        .and(TeamCardSpecification.hasStream());
    var organizationId = organizationContext.getOrganizationId();
    if (organizationId.isPresent() && !organizationContext.isOverlord()) {
      specs = specs.and(TeamCardSpecification.organizationEquals(organizationId.get()));
    }
    var page = teamCardsService.findAll(specs, pageable)
        .map(teamCardMapper::mapToDto);
    return ResponseEntity.ok(new PagedModel<>(page));
  }

  @Override
  public ResponseEntity<TeamCardDto> getTeamCard(UUID id, String username) {
    var teamCard = teamCardsService.getTeamCard(id, username);
    organizationContext.checkOrganizationAccess(teamCard.getOrganizationId());
    return ResponseEntity.ok(teamCardMapper.mapToDto(teamCard));
  }

  @Override
  public ResponseEntity<Void> deleteTeamCard(UUID id, String username) {
    var teamCard = teamCardsService.getTeamCard(id, username);
    organizationContext.checkOrganizationAccess(teamCard.getOrganizationId());
    teamCardsService.deleteTeamCard(id, username);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<Map<String, String>>> getTeamsByUser(String username) {
    log.info("Getting teams for user: {}", username);
    List<TeamCard> teams = teamCardsService.getTeamCardsByUser(username);
    var organizationId = organizationContext.getOrganizationId();
    if (organizationId.isPresent() && !organizationContext.isOverlord()) {
      teams = teams.stream()
          .filter(team -> organizationId.get().equals(team.getOrganizationId()))
          .toList();
    }

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
    
    var organizationId = organizationContext.getOrganizationId();
    teamCardsService.reassignTeams(fromUsername, toUsername, toUserFullName,
        organizationId.filter(id -> !organizationContext.isOverlord()).orElse(null));
    
    log.info("Successfully reassigned teams from '{}' to '{}'", fromUsername, toUsername);
    return ResponseEntity.ok().build();
  }
}
