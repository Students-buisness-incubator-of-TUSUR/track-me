package net.trackme.backend.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.backend.services.stream.MutableStreamService;
import net.trackme.backend.services.teamcard.TeamCardsService;
import net.trackme.backend.services.teamcard.report.TeamCardsReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Тесты организационной области в use case карточек команд.
 */
class TeamCardsUseCaseOrganizationTest {

  private static final UUID ORGANIZATION_ID = UUID.randomUUID();

  private static final String ORGANIZATION_NAME = "Организация А";

  private static final UUID STREAM_ID = UUID.randomUUID();

  private static final UUID TEAM_CARD_ID = UUID.randomUUID();

  private final TeamCardsService teamCardsService = mock(TeamCardsService.class);

  private final MutableStreamService streamService = mock(MutableStreamService.class);

  private final TeamCardsReportService reportService = mock(TeamCardsReportService.class);

  private final TeamCardMapper teamCardMapper = mock(TeamCardMapper.class);

  private final NtiMarketService ntiMarketService = mock(NtiMarketService.class);

  private final OrganizationContext organizationContext = mock(OrganizationContext.class);

  private final TeamCardsUseCase useCase = new TeamCardsUseCase(
      teamCardsService, streamService, reportService, teamCardMapper,
      ntiMarketService, organizationContext);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createTeamCardChecksAccessAndTakesOrganizationFromStream() {
    var dto = mock(TeamCardCreateDto.class);
    when(dto.ntiMarketIds()).thenReturn(List.of());
    var stream = stream();
    var entity = new TeamCard();
    when(streamService.findActive(STREAM_ID)).thenReturn(stream);
    when(teamCardMapper.mapToEntity(dto)).thenReturn(entity);
    when(ntiMarketService.getNtiMarkets(anyList())).thenReturn(List.of());
    when(teamCardsService.createTeamCard(entity)).thenReturn(entity);

    useCase.createTeamCard(dto, STREAM_ID, authentication("tracker"));

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
    assertThat(entity.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(entity.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
  }

  @Test
  void createTeamCardFallsBackToCurrentOrganization() {
    var dto = mock(TeamCardCreateDto.class);
    when(dto.ntiMarketIds()).thenReturn(List.of());
    var stream = new Stream();
    var entity = new TeamCard();
    when(streamService.findActive(STREAM_ID)).thenReturn(stream);
    when(teamCardMapper.mapToEntity(dto)).thenReturn(entity);
    when(ntiMarketService.getNtiMarkets(anyList())).thenReturn(List.of());
    when(teamCardsService.createTeamCard(entity)).thenReturn(entity);
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.getOrganizationName()).thenReturn(Optional.of(ORGANIZATION_NAME));

    useCase.createTeamCard(dto, STREAM_ID, authentication("tracker"));

    assertThat(entity.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(entity.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
  }

  @Test
  void updateTeamCardChecksOrganizationAccess() {
    var existing = new TeamCard();
    existing.setOrganizationId(ORGANIZATION_ID);
    var dto = mock(TeamCardUpdateDto.class);
    when(dto.ntiMarketIds()).thenReturn(List.of());
    var mapped = new TeamCard();
    when(teamCardsService.getTeamCard(TEAM_CARD_ID)).thenReturn(existing);
    when(teamCardMapper.mapToEntity(dto)).thenReturn(mapped);
    when(ntiMarketService.getNtiMarkets(anyList())).thenReturn(List.of());
    when(teamCardsService.updateTeamCard(TEAM_CARD_ID, mapped)).thenReturn(mapped);

    useCase.updateTeamCard(TEAM_CARD_ID, dto);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  @Test
  void getTeamCardsFiltersByOrganizationForRegularUser() {
    var pageable = PageRequest.of(0, 10);
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);
    when(teamCardsService.getTeamCards(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    var result = useCase.getTeamCards(List.of(), authentication("tracker"), pageable);

    assertThat(result).isEmpty();
    verify(teamCardsService).getTeamCards(any(Specification.class), eq(pageable));
  }

  @Test
  void getTeamCardChecksOrganizationAccess() {
    var teamCard = new TeamCard();
    teamCard.setOrganizationId(ORGANIZATION_ID);
    when(teamCardsService.getTeamCard(TEAM_CARD_ID)).thenReturn(teamCard);

    useCase.getTeamCard(TEAM_CARD_ID);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  @Test
  void deleteTeamCardChecksOrganizationAccess() {
    var teamCard = new TeamCard();
    teamCard.setOrganizationId(ORGANIZATION_ID);
    when(teamCardsService.getTeamCard(TEAM_CARD_ID)).thenReturn(teamCard);

    useCase.deleteTeamCard(TEAM_CARD_ID);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
    verify(teamCardsService).deleteTeamCard(TEAM_CARD_ID);
  }

  @Test
  void getTeamCardCountChecksStreamOrganizationAccess() {
    when(streamService.getById(STREAM_ID)).thenReturn(stream());
    when(teamCardsService.getTeamCardCount(STREAM_ID)).thenReturn(3);

    var count = useCase.getTeamCardCount(STREAM_ID);

    assertThat(count).isEqualTo(3);
    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  private static Stream stream() {
    var stream = new Stream();
    stream.setOrganizationId(ORGANIZATION_ID);
    stream.setOrganizationName(ORGANIZATION_NAME);
    return stream;
  }

  private static Authentication authentication(String username) {
    var authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(username);
    return authentication;
  }
}
