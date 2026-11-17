package net.trackme.backend.services.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.repos.StreamRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Тесты каскадного удаления данных организации в backend.
 */
class OrganizationDataServiceTest {

  private final TeamCardsRepository teamCards = mock(TeamCardsRepository.class);

  private final StreamRepository streams = mock(StreamRepository.class);

  private final AclService aclService = mock(AclService.class);

  private final OrganizationDataService service =
      new OrganizationDataService(teamCards, streams, aclService);

  @Test
  void deletesTeamCardsStreamsAndAclEntries() {
    var organizationId = UUID.randomUUID();
    var cardFromOrganization = card();
    var cardFromStream = card();
    var stream = new Stream();
    stream.setId(UUID.randomUUID());

    when(teamCards.findAllByOrganizationId(organizationId))
        .thenReturn(List.of(cardFromOrganization));
    when(teamCards.findAllByStreamsOrganizationId(organizationId))
        .thenReturn(List.of(cardFromStream, cardFromOrganization));
    when(streams.findAllByOrganizationId(organizationId)).thenReturn(List.of(stream));

    service.deleteOrganizationData(organizationId);

    verify(aclService).deleteAcl(cardFromOrganization);
    verify(aclService).deleteAcl(cardFromStream);
    verify(streams).deleteAll(List.of(stream));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<TeamCard>> deletedCards =
        ArgumentCaptor.forClass(Iterable.class);
    verify(teamCards).deleteAll(deletedCards.capture());
    assertThat(deletedCards.getValue())
        .containsExactlyInAnyOrder(cardFromOrganization, cardFromStream);
    verify(teamCards).flush();
  }

  @Test
  void renameUpdatesStreamsAndTeamCards() {
    var organizationId = UUID.randomUUID();
    when(streams.updateOrganizationName(organizationId, "Новое название")).thenReturn(2);
    when(teamCards.updateOrganizationName(organizationId, "Новое название")).thenReturn(3);

    service.renameOrganization(organizationId, "Новое название");

    verify(streams).updateOrganizationName(organizationId, "Новое название");
    verify(teamCards).updateOrganizationName(organizationId, "Новое название");
  }

  @Test
  void missingAclDoesNotStopCascadeDeletion() {
    var organizationId = UUID.randomUUID();
    var card = card();
    when(teamCards.findAllByOrganizationId(organizationId)).thenReturn(List.of(card));
    when(teamCards.findAllByStreamsOrganizationId(organizationId)).thenReturn(List.of());
    when(streams.findAllByOrganizationId(organizationId)).thenReturn(List.of());
    doThrow(new IllegalStateException("ACL not found"))
        .when(aclService).deleteAcl(any());

    service.deleteOrganizationData(organizationId);

    verify(teamCards).deleteAll(any(Iterable.class));
    verify(streams).deleteAll(java.util.Collections.<Stream>emptyList());
  }

  private static TeamCard card() {
    var card = new TeamCard();
    card.setId(UUID.randomUUID());
    return card;
  }
}
