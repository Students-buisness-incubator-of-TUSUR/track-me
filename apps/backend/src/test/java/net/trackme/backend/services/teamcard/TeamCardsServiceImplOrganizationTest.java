package net.trackme.backend.services.teamcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.stream.StreamService;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Тесты организационных сценариев сервиса карточек команд.
 */
class TeamCardsServiceImplOrganizationTest {

  private final TeamCardsRepository teamCardsRepository = mock(TeamCardsRepository.class);

  private final StreamService streamService = mock(StreamService.class);

  private final AclService aclService = mock(AclService.class);

  private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

  private final TeamCardEventsProducer eventsProducer = mock(TeamCardEventsProducer.class);

  private final TeamCardsServiceImpl service = new TeamCardsServiceImpl(
      teamCardsRepository, streamService, aclService, eventPublisher, eventsProducer);

  @Test
  void updateTeamCardWithStreamCopiesStreamOrganization() {
    var teamCardId = UUID.randomUUID();
    var streamId = UUID.randomUUID();
    var organizationId = UUID.randomUUID();
    var existing = TeamCard.builder()
        .id(teamCardId)
        .name("Команда")
        .username("tracker")
        .build();
    var update = TeamCard.builder().name("Обновлённая команда").build();
    var stream = new Stream();
    stream.setId(streamId);
    stream.setOrganizationId(organizationId);
    stream.setOrganizationName("Организация А");

    when(teamCardsRepository.findById(teamCardId)).thenReturn(Optional.of(existing));
    when(streamService.getById(streamId)).thenReturn(stream);
    when(teamCardsRepository.save(existing)).thenReturn(existing);

    var updated = service.updateTeamCard(teamCardId, update, streamId, null);

    assertThat(updated.getOrganizationId()).isEqualTo(organizationId);
    assertThat(updated.getOrganizationName()).isEqualTo("Организация А");
  }

  @Test
  void updateTeamCardDoesNotChangeOrganizationWhenStreamHasNone() {
    var teamCardId = UUID.randomUUID();
    var streamId = UUID.randomUUID();
    var existingOrganization = UUID.randomUUID();
    var existing = TeamCard.builder()
        .id(teamCardId)
        .name("Команда")
        .username("tracker")
        .organizationId(existingOrganization)
        .organizationName("Прежняя организация")
        .build();
    var stream = new Stream();
    stream.setId(streamId);

    when(teamCardsRepository.findById(teamCardId)).thenReturn(Optional.of(existing));
    when(streamService.getById(streamId)).thenReturn(stream);
    when(teamCardsRepository.save(existing)).thenReturn(existing);

    var updated = service.updateTeamCard(
        teamCardId, TeamCard.builder().build(), streamId, null);

    assertThat(updated.getOrganizationId()).isEqualTo(existingOrganization);
    assertThat(updated.getOrganizationName()).isEqualTo("Прежняя организация");
  }

  @Test
  void reassignTeamsLimitsSelectionByOrganization() {
    var organizationId = UUID.randomUUID();
    var team = TeamCard.builder()
        .id(UUID.randomUUID())
        .name("Команда")
        .username("old")
        .trackerFullName("Старый Трекер")
        .build();
    when(teamCardsRepository.findByUsernameAndOrganizationId("old", organizationId))
        .thenReturn(List.of(team));

    service.reassignTeams("old", "new", "Новый Трекер", organizationId);

    assertThat(team.getUsername()).isEqualTo("new");
    assertThat(team.getTrackerFullName()).isEqualTo("Новый Трекер");
    verify(teamCardsRepository).saveAll(List.of(team));
    verify(aclService).updateAclOwner(team, "new");
  }

  @Test
  void reassignTeamsWithoutOrganizationUsesFullNameFallback() {
    var team = TeamCard.builder()
        .id(UUID.randomUUID())
        .name("Команда")
        .username("old")
        .build();
    when(teamCardsRepository.findByUsername("old")).thenReturn(List.of(team));

    service.reassignTeams("old", "new", "  ", null);

    assertThat(team.getUsername()).isEqualTo("new");
    assertThat(team.getTrackerFullName()).isEqualTo("new");
    verify(teamCardsRepository).saveAll(List.of(team));
  }
}
