package net.trackme.meetingservice.messaging;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.messaging.backend.TeamCardEventConsumer;
import net.trackme.meetingservice.messaging.backend.TeamCardStreamAddedEvent;
import net.trackme.meetingservice.messaging.backend.TeamCardStreamRemovedEvent;
import net.trackme.meetingservice.messaging.backend.TeamCardUpdatedEvent;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCardEventConsumerTest {

    @Mock
    private MeetingMetadataRepository metadataRepository;

    @Mock
    private SsoApiClient ssoApiClient;

    @InjectMocks
    private TeamCardEventConsumer teamCardEventConsumer;

    @Test
    void handleTeamCardUpdated_withNewTracker_trackerFoundInSso() {
        UUID teamId = UUID.randomUUID();
        String username = "new_tracker";
        Boolean newPassive = true; // 👈 Добавили проверку на true/false
        var event = new TeamCardUpdatedEvent(teamId, "New Name", username, newPassive, "Ivan Ivanov");

        var tracker = UserDto.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .fullName("Ivan Ivanov")
                .build();

        when(ssoApiClient.getTrackers()).thenReturn(List.of(tracker));

        teamCardEventConsumer.handleTeamCardUpdated(event);

        // 👇 ДОБАВИТЬ ЭТУ СТРОКУ
        verify(metadataRepository).updatePassiveFlag(teamId, newPassive);

        verify(metadataRepository).updateMetadata(
                teamId, "New Name", username, tracker.getId(), tracker.getFullName()
        );
    }

    @Test
    void handleTeamCardUpdated_withNewTracker_trackerNotFoundInSso() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        String username = "unknown_user";
        Boolean newPassive = false;
        var event = new TeamCardUpdatedEvent(teamId, "Name", username, newPassive, null);

        when(ssoApiClient.getTrackers()).thenReturn(List.of());

        // Act
        teamCardEventConsumer.handleTeamCardUpdated(event);

        // Assert
        verify(metadataRepository).updateMetadata(teamId, "Name", username, null, null);
    }

    @Test
    void handleTeamCardUpdated_ssoApiThrowsException_gracefulHandling() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = true;
        var event = new TeamCardUpdatedEvent(teamId, "Name", "error_user", newPassive, null);

        when(ssoApiClient.getTrackers()).thenThrow(new RuntimeException("SSO Down"));

        // Act
        teamCardEventConsumer.handleTeamCardUpdated(event);

        // Assert
        verify(metadataRepository).updateMetadata(eq(teamId), anyString(), anyString(), any(), any());
    }

    @Test
    void handleTeamCardUpdated_usernameIsNull_skipsSsoCall() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = false;
        var event = new TeamCardUpdatedEvent(teamId, "New Name", null, newPassive, null);

        // Act
        teamCardEventConsumer.handleTeamCardUpdated(event);

        // Assert
        verifyNoInteractions(ssoApiClient);
        verify(metadataRepository).updateMetadata(teamId, "New Name", null, null, null);
    }

    @Test
    void handleStreamAdded_success() {
        UUID teamId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        var event = new TeamCardStreamAddedEvent(teamId, streamId);

        teamCardEventConsumer.handleStreamAdded(event);

        verify(metadataRepository).addStreamToTeamMeetings(teamId, streamId);
    }

    @Test
    void handleStreamRemoved_success() {
        UUID teamId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        var event = new TeamCardStreamRemovedEvent(teamId, streamId);

        teamCardEventConsumer.handleStreamRemoved(event);

        verify(metadataRepository).removeStreamFromTeamMeetings(teamId, streamId);
    }
}