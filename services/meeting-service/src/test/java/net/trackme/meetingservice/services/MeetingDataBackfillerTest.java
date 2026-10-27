package net.trackme.meetingservice.services;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingDataBackfillerTest {

    @Mock private MeetingMetadataRepository metadataRepository;
    @Mock private SsoApiClient ssoApiClient;
    @Mock(name = "userBackendApiClient") private BackendApiClient userBackendClient;

    @InjectMocks
    private MeetingDataBackfiller backfiller;

    @Test
    void testPhase1_RepairIncompleteData() {
        UUID teamId = UUID.randomUUID();
        Meeting meeting = new Meeting();
        meeting.setTeamCardId(teamId);
        meeting.setTrackerUsername("user1");
        AtomicBoolean flag = new AtomicBoolean(false);

        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of(teamId));
        when(metadataRepository.findAllIncompleteByTeamCardId(teamId)).thenReturn(List.of(meeting));
        when(userBackendClient.getTeamCardById(teamId)).thenReturn(
                TeamCardDto.builder().name("Team").username("user1").streams(List.of()).build()
        );

        when(ssoApiClient.getTrackers()).thenReturn(List.of());
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of(teamId));

        backfiller.run("token", flag);

        verify(metadataRepository, atLeastOnce()).saveAll(anyList());
        assertTrue(flag.get());
    }

    @Test
    void testPhase2_SyncTrackerNames() {
        UUID teamId = UUID.randomUUID();
        String username = "tracker1";
        AtomicBoolean flag = new AtomicBoolean(false);

        Meeting meeting = new Meeting();
        meeting.setTeamCardId(teamId);
        meeting.setTrackerUsername(username);
        meeting.setTrackerFullName("Old Name");

        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of());
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of(teamId));

        when(userBackendClient.getTeamCardById(teamId)).thenReturn(
                TeamCardDto.builder().username(username).streams(List.of()).build()
        );

        when(ssoApiClient.getTrackers()).thenReturn(List.of(
                UserDto.builder().username(username).fullName("New Name").id("some-uuid").build()
        ));

        when(metadataRepository.findAllByTeamCardId(teamId)).thenReturn(List.of(meeting));

        backfiller.run("token", flag);

        verify(metadataRepository, atLeastOnce()).saveAll(anyList());
        assertTrue(flag.get());
    }

    @Test
    void testHandleExceptions() {
        AtomicBoolean flag = new AtomicBoolean(false);

        when(ssoApiClient.getTrackers()).thenThrow(new RuntimeException("SSO Down"));

        assertThrows(RuntimeException.class, () -> backfiller.run("token", flag));

        verify(metadataRepository, never()).saveAll(anyList());
        assertFalse(flag.get());
    }

    @Test
    void testAlreadyStarted_SkipsExecution() {
        AtomicBoolean flag = new AtomicBoolean(true);

        backfiller.run("token", flag);

        verifyNoInteractions(ssoApiClient);
        verifyNoInteractions(metadataRepository);
    }

    @Test
    void leavesSynchronizedMeetingsUntouched() {
        UUID teamId = UUID.randomUUID();
        Meeting meeting = new Meeting();
        meeting.setTeamName("Team");
        meeting.setTrackerUsername("tracker1");
        meeting.setTrackerFullName("tracker1");
        meeting.setStreamIds(Set.of());
        meeting.updateTeamStatusValue();
        when(ssoApiClient.getTrackers()).thenReturn(List.of());
        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of());
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of(teamId));
        when(userBackendClient.getTeamCardById(teamId)).thenReturn(
                TeamCardDto.builder().name("Team").username("tracker1").streams(List.of()).build());
        when(metadataRepository.findAllByTeamCardId(teamId)).thenReturn(List.of(meeting));

        backfiller.run("token", new AtomicBoolean(false));

        verify(metadataRepository, never()).saveAll(anyList());
    }

    @Test
    void recalculatesWeightsForEveryMeetingEvenAfterEarlierMeetingChanged() {
        UUID teamId = UUID.randomUUID();
        Meeting first = new Meeting();
        Meeting second = new Meeting();
        for (Meeting meeting : List.of(first, second)) {
            meeting.setTeamName("Team");
            meeting.setTrackerUsername("tracker1");
            meeting.setTrackerFullName("tracker1");
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }
        when(ssoApiClient.getTrackers()).thenReturn(List.of());
        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of());
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of(teamId));
        when(userBackendClient.getTeamCardById(teamId)).thenReturn(
                TeamCardDto.builder().name("Team").username("tracker1").streams(List.of()).build());
        when(metadataRepository.findAllByTeamCardId(teamId)).thenReturn(List.of(first, second));

        backfiller.run("token", new AtomicBoolean(false));

        verify(metadataRepository).saveAll(List.of(first, second));
        assertEquals(0, first.getTeamStatusValue().signum());
        assertEquals(0, second.getTeamStatusValue().signum());
    }

    @Test
    void repairsStreamMembershipTrackerIdentityAndWeightTogether() {
        UUID teamId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();
        Meeting meeting = new Meeting();
        meeting.setTeamName("Old team");
        meeting.setTrackerUsername("old-tracker");
        meeting.setTrackerFullName("Old Name");
        meeting.setStreamIds(Set.of(UUID.randomUUID()));
        meeting.setStatus(MeetingStatus.COMPLETED);
        meeting.setTeamStatus(TeamStatus.OK);
        meeting.setTeamStatusValue(BigDecimal.ZERO);
        when(ssoApiClient.getTrackers()).thenReturn(List.of(UserDto.builder()
                .username("tracker1").fullName("Tracker Name").id("tracker-id").build()));
        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of(teamId));
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of(teamId));
        when(userBackendClient.getTeamCardById(teamId)).thenReturn(TeamCardDto.builder()
                .name("New team").username("tracker1").streams(List.of(new StreamDto(streamId))).build());
        when(metadataRepository.findAllIncompleteByTeamCardId(teamId)).thenReturn(List.of(meeting));

        backfiller.run("token", new AtomicBoolean(false));

        verify(metadataRepository).saveAll(List.of(meeting));
        assertEquals("New team", meeting.getTeamName());
        assertEquals("tracker1", meeting.getTrackerUsername());
        assertEquals("Tracker Name", meeting.getTrackerFullName());
        assertEquals("tracker-id", meeting.getTrackerId());
        assertEquals(Set.of(streamId), meeting.getStreamIds());
        assertEquals(0, BigDecimal.ONE.compareTo(meeting.getTeamStatusValue()));
    }
}
