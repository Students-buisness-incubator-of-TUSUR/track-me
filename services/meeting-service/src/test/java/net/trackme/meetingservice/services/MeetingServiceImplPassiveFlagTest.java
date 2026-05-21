package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimplePassiveFlagTest {

    @Mock private MeetingMapper meetingMapper;
    @Mock private MeetingRepository meetingRepository;
    @Mock private AclService aclService;
    @Mock private MeetingEventsProducer meetingEventsProducer;
    @Mock private BackendApiClient userBackendClient;
    @Mock private SsoApiClient ssoApiClient;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    @Test
    void testPassiveFlagBlocksCreation() {
        UUID teamId = UUID.randomUUID();

        TeamCardDto teamCard = TeamCardDto.builder()
                .id(teamId)
                .passive(true)
                .streams(new ArrayList<>())
                .build();

        MeetingCreateDto createDto = MeetingCreateDto.builder()
                .startDate(OffsetDateTime.now())
                .build();

        when(userBackendClient.getTeamCardById(teamId)).thenReturn(teamCard);
        when(meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                any(), any(), any())).thenReturn(false);

        // Проверяем, что при пассивной команде выбрасывается исключение
        assertThatThrownBy(() -> meetingService.createMeeting(teamId, createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testNonPassiveFlagAllowsCreation() {
        UUID teamId = UUID.randomUUID();

        TeamCardDto teamCard = TeamCardDto.builder()
                .id(teamId)
                .passive(false)
                .name("Test Team")
                .username("tracker")
                .streams(new ArrayList<>())
                .build();

        MeetingCreateDto createDto = MeetingCreateDto.builder()
                .startDate(OffsetDateTime.now())
                .tasksCurrentMeeting("task1")
                .tasksNextMeeting("task2")
                .build();

        Meeting meeting = Meeting.builder()
                .id(UUID.randomUUID())
                .teamCardId(teamId)
                .build();

        when(userBackendClient.getTeamCardById(teamId)).thenReturn(teamCard);
        when(meetingMapper.mapToEntity(createDto)).thenReturn(meeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(ssoApiClient.getTrackers()).thenReturn(Collections.emptyList());
        when(meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                any(), any(), any())).thenReturn(false);

        // Должно работать без ошибок
        meetingService.createMeeting(teamId, createDto);

        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }
}