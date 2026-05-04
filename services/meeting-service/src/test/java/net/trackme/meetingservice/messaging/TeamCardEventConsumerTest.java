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
        // Arrange
        UUID teamId = UUID.randomUUID();
        String username = "new_tracker";
        var event = new TeamCardUpdatedEvent(teamId, "New Name", username);

        var tracker = UserDto.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .fullName("Ivan Ivanov")
                .build();

        when(ssoApiClient.getTrackers()).thenReturn(List.of(tracker));

        // Act
        teamCardEventConsumer.handleTeamCardUpdated(event);

        // Assert
        verify(metadataRepository).updateMetadata(
                teamId, "New Name", username, tracker.getId(), tracker.getFullName()
        );
    }

    @Test
    void handleTeamCardUpdated_withNewTracker_trackerNotFoundInSso() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        String username = "unknown_user";
        var event = new TeamCardUpdatedEvent(teamId, "Name", username);

        when(ssoApiClient.getTrackers()).thenReturn(List.of()); // Пустой список из SSO

        // Act
        teamCardEventConsumer.handleTeamCardUpdated(event);

        // Assert
        verify(metadataRepository).updateMetadata(teamId, "Name", username, null, null);
    }

    @Test
    void handleTeamCardUpdated_ssoApiThrowsException_gracefulHandling() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        var event = new TeamCardUpdatedEvent(teamId, "Name", "error_user");

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
        var event = new TeamCardUpdatedEvent(teamId, "New Name", null);

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