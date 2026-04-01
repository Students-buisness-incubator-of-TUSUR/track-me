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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of(teamId));
        when(metadataRepository.findAllIncompleteByTeamCardId(teamId)).thenReturn(List.of(meeting));
        when(userBackendClient.getTeamCardById(teamId)).thenReturn(
                TeamCardDto.builder().name("Team").username("user1").streams(List.of()).build()
        );

        when(ssoApiClient.getTrackers()).thenReturn(List.of());
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of(teamId));

        backfiller.run("token");

        verify(metadataRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void testPhase2_SyncTrackerNames() {
        UUID teamId = UUID.randomUUID();
        String username = "tracker1";

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

        // ИСПРАВЛЕНО: Твой код вызывает этот метод у metadataRepository, а не у meetingRepository
        when(metadataRepository.findAllByTeamCardId(teamId)).thenReturn(List.of(meeting));

        backfiller.run("token");

        // Проверяем сохранение через metadataRepository, как того требует лог
        verify(metadataRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void testHandleExceptions() {
        UUID teamId = UUID.randomUUID();
        when(metadataRepository.findTeamIdsWithIncompleteMetadata()).thenReturn(List.of(teamId));
        when(userBackendClient.getTeamCardById(teamId)).thenThrow(new RuntimeException("Error"));

        when(ssoApiClient.getTrackers()).thenReturn(List.of());
        when(metadataRepository.findAllUniqueTeamCardIds()).thenReturn(List.of());

        backfiller.run("token");

        verify(metadataRepository, never()).saveAll(anyList());
    }
}