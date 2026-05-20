package net.trackme.meetingservice.messaging.backend;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCardEventConsumerPassiveTest {

    @Mock
    private MeetingMetadataRepository metadataRepository;

    @Mock
    private SsoApiClient ssoApiClient;

    @InjectMocks
    private TeamCardEventConsumer teamCardEventConsumer;

    @Test
    void handleTeamCardUpdated_shouldCallUpdatePassiveFlag() {
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = true;

        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                teamId, "Name", "user", newPassive, "FullName"
        );

        when(ssoApiClient.getTrackers()).thenReturn(java.util.Collections.emptyList());

        teamCardEventConsumer.handleTeamCardUpdated(event);

        verify(metadataRepository).updatePassiveFlag(teamId, newPassive);
    }

    @Test
    void handleTeamCardUpdated_withNewPassiveFalse_shouldCallUpdatePassiveFlag() {
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = false;

        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                teamId, "Name", "user", newPassive, null
        );

        when(ssoApiClient.getTrackers()).thenReturn(java.util.Collections.emptyList());

        teamCardEventConsumer.handleTeamCardUpdated(event);

        verify(metadataRepository).updatePassiveFlag(teamId, newPassive);
    }
}