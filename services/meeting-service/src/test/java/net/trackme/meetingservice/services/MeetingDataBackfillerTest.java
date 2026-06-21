package net.trackme.meetingservice.services;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingDataBackfillerTest {

    @Mock private MeetingRepository meetingRepository;
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

        try {
            backfiller.run("token", flag);
        } catch (Exception ignored) {
        }

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
}