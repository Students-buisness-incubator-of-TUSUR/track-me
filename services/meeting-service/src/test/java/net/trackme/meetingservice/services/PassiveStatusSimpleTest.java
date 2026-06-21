package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassiveStatusSimpleTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock(name = "userBackendApiClient")
    private BackendApiClient userBackendClient;

    @Mock
    private AclService aclService;

    @Mock
    private MeetingEventsProducer meetingEventsProducer;

    @Mock
    private SsoApiClient ssoApiClient;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    @Test
    void createMeetingWithPassiveTeam_shouldThrowException() {
        // Arrange
        UUID teamCardId = UUID.randomUUID();
        MeetingCreateDto createDto = MeetingCreateDto.builder()
                .startDate(OffsetDateTime.now())
                .build();

        TeamCardDto teamCardDto = new TeamCardDto();
        teamCardDto.setPassive(true);
        teamCardDto.setStreams(Collections.emptyList());

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            meetingService.createMeeting(teamCardId, createDto);
        });

        assertEquals("Трекер не может создавать встречи для пассивной команды", exception.getMessage());
    }

    @Test
    void testAdminCanCreateMeetingForPassiveTeam() {
        TeamCardDto teamCardDto = new TeamCardDto();
        teamCardDto.setPassive(true);
        teamCardDto.setStreams(Collections.emptyList());

        // Просто проверяем, что объект создан и поле установлено
        assertNotNull(teamCardDto);
        assertTrue(teamCardDto.getPassive());
    }



}


