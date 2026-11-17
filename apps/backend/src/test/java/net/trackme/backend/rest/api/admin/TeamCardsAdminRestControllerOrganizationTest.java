package net.trackme.backend.rest.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.backend.services.stream.StreamService;
import net.trackme.backend.services.teamcard.TeamCardsService;
import net.trackme.commons.filters.FilterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

/**
 * Тесты организационной области в административном API карточек команд.
 */
class TeamCardsAdminRestControllerOrganizationTest {

  private static final UUID ORGANIZATION_ID = UUID.randomUUID();

  private static final UUID STREAM_ID = UUID.randomUUID();

  private static final UUID TEAM_CARD_ID = UUID.randomUUID();

  private final TeamCardsService teamCardsService = mock(TeamCardsService.class);

  private final TeamCardMapper teamCardMapper = mock(TeamCardMapper.class);

  private final NtiMarketService ntiMarketService = mock(NtiMarketService.class);

  private final StreamService streamService = mock(StreamService.class);

  private final OrganizationContext organizationContext = mock(OrganizationContext.class);

  private final TeamCardsAdminRestControllerImpl controller =
      new TeamCardsAdminRestControllerImpl(teamCardsService, teamCardMapper,
          ntiMarketService, streamService, organizationContext);

  @Test
  void getTeamsByUserFiltersTeamsOfOtherOrganizations() {
    var ownTeam = teamCard("Своя команда", ORGANIZATION_ID);
    var foreignTeam = teamCard("Чужая команда", UUID.randomUUID());
    when(teamCardsService.getTeamCardsByUser("tracker"))
        .thenReturn(List.of(ownTeam, foreignTeam));
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);

    var response = controller.getTeamsByUser("tracker");

    assertThat(response.getBody())
        .containsExactly(Map.of("id", ownTeam.getId().toString(), "name", "Своя команда"));
  }

  @Test
  void getTeamsByUserReturnsAllTeamsForOverlord() {
    var first = teamCard("Первая", ORGANIZATION_ID);
    var second = teamCard("Вторая", UUID.randomUUID());
    when(teamCardsService.getTeamCardsByUser("tracker")).thenReturn(List.of(first, second));
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(true);

    var response = controller.getTeamsByUser("tracker");

    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void reassignTeamsPassesCurrentOrganizationForRegularUser() {
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);

    var response = controller.reassignTeams(
        new TeamCardsAdminRestController.ReassignTeamsRequest("old", "new", "Новый Трекер"));

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    verify(teamCardsService).reassignTeams("old", "new", "Новый Трекер", ORGANIZATION_ID);
  }

  @Test
  void reassignTeamsDoesNotLimitOrganizationForOverlord() {
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(true);

    controller.reassignTeams(
        new TeamCardsAdminRestController.ReassignTeamsRequest("old", "new", null));

    verify(teamCardsService).reassignTeams("old", "new", null, null);
  }

  @Test
  void updateTeamCardChecksOrganizationOfTeamCardAndStream() {
    var existing = teamCard("Команда", ORGANIZATION_ID);
    var stream = new Stream();
    stream.setId(STREAM_ID);
    stream.setOrganizationId(ORGANIZATION_ID);
    var updateDto = mock(TeamCardUpdateDto.class);
    when(updateDto.ntiMarketIds()).thenReturn(List.of());
    when(teamCardsService.getTeamCard(TEAM_CARD_ID)).thenReturn(existing);
    when(streamService.getById(STREAM_ID)).thenReturn(stream);
    when(teamCardMapper.mapToEntity(updateDto)).thenReturn(new TeamCard());
    when(ntiMarketService.getNtiMarkets(List.of())).thenReturn(List.of());
    when(teamCardsService.updateTeamCard(
        org.mockito.ArgumentMatchers.eq(TEAM_CARD_ID),
        org.mockito.ArgumentMatchers.any(TeamCard.class),
        org.mockito.ArgumentMatchers.eq(STREAM_ID),
        org.mockito.ArgumentMatchers.eq("tracker")))
        .thenReturn(existing);

    var response = controller.updateTeamCard(TEAM_CARD_ID, "tracker", STREAM_ID, updateDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(organizationContext, times(2)).checkOrganizationAccess(ORGANIZATION_ID);
  }

  @Test
  void getTeamCardsFiltersByCurrentOrganization() {
    var pageable = PageRequest.of(0, 10);
    var filterRequest = mock(FilterRequest.class);
    when(filterRequest.filters()).thenReturn(List.of());
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);
    when(teamCardsService.findAll(
        org.mockito.ArgumentMatchers.any(Specification.class),
        org.mockito.ArgumentMatchers.eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    var response = controller.getTeamCards(pageable, filterRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    verify(teamCardsService).findAll(
        org.mockito.ArgumentMatchers.any(Specification.class),
        org.mockito.ArgumentMatchers.eq(pageable));
  }

  @Test
  void getTeamCardChecksOrganizationAccess() {
    var teamCard = teamCard("Команда", ORGANIZATION_ID);
    when(teamCardsService.getTeamCard(TEAM_CARD_ID, "tracker")).thenReturn(teamCard);

    var response = controller.getTeamCard(TEAM_CARD_ID, "tracker");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  private static TeamCard teamCard(String name, UUID organizationId) {
    var teamCard = new TeamCard();
    teamCard.setId(UUID.randomUUID());
    teamCard.setName(name);
    teamCard.setOrganizationId(organizationId);
    return teamCard;
  }
}
