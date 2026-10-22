package net.trackme.backend.services.teamcard;

import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.stream.StreamService;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TeamCardStreamRequiredTest {
    @Test
    void missingStreamIsRejectedBeforeSavingOrPublishingEvents() {
        var repository = mock(TeamCardsRepository.class);
        var streams = mock(StreamService.class);
        var acl = mock(AclService.class);
        var events = mock(ApplicationEventPublisher.class);
        var service = new TeamCardsServiceImpl(repository, streams, acl, events);
        var team = new TeamCard();

        assertThrows(IllegalArgumentException.class, () -> service.createTeamCard(team, null, "tracker1"));

        verifyNoInteractions(repository, streams, acl, events);
    }
}
