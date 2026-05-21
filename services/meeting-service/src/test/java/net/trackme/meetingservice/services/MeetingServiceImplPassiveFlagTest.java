package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
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
class MeetingServiceImplPassiveFlagTest {

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private AclService aclService;

    @Mock
    private MeetingEventsProducer meetingEventsProducer;

    @Mock
    private BackendApiClient userBackendClient;

    @Mock
    private SsoApiClient ssoApiClient;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private UUID teamCardId;
    private UUID meetingId;
    private MeetingCreateDto createDto;
    private MeetingUpdateDto updateDto;
    private Meeting meeting;
    private TeamCardDto teamCardDto;

    @BeforeEach
    void setUp() {
        teamCardId = UUID.randomUUID();
        meetingId = UUID.randomUUID();

        createDto = MeetingCreateDto.builder()
                .startDate(OffsetDateTime.now())
                .tasksCurrentMeeting("task1")
                .tasksNextMeeting("task2")
                .build();

        updateDto = MeetingUpdateDto.builder()
                .tasksCurrentMeeting("updated task")
                .build();

        meeting = Meeting.builder()
                .id(meetingId)
                .teamCardId(teamCardId)
                .status(MeetingStatus.SCHEDULED)
                .build();

        teamCardDto = TeamCardDto.builder()
                .id(teamCardId)
                .name("Test Team")
                .username("tracker1")
                .passive(true)
                .streams(new ArrayList<>())
                .build();
    }

    @Test
    void createMeeting_ShouldThrow_WhenTeamCardPassive() {
        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                any(), any(), any())).thenReturn(false);

        // Когда команда пассивная - должна быть ошибка
        // (роль пользователя не ADMIN/SUPER_ADMIN)
        assertThatThrownBy(() -> meetingService.createMeeting(teamCardId, createDto))
                .isInstanceOf(Exception.class); // Любое исключение

        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void createMeeting_ShouldWork_WhenTeamCardNotPassive() {
        teamCardDto.setPassive(false);

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingMapper.mapToEntity(createDto)).thenReturn(meeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(ssoApiClient.getTrackers()).thenReturn(Collections.emptyList());
        when(meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                any(), any(), any())).thenReturn(false);

        // Должно работать без ошибок
        meetingService.createMeeting(teamCardId, createDto);

        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    @Test
    void createMeeting_ShouldWork_WhenPassiveIsNull() {
        teamCardDto.setPassive(null);

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingMapper.mapToEntity(createDto)).thenReturn(meeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(ssoApiClient.getTrackers()).thenReturn(Collections.emptyList());
        when(meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                any(), any(), any())).thenReturn(false);

        meetingService.createMeeting(teamCardId, createDto);

        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    @Test
    void updateMeeting_ShouldThrow_WhenTeamCardPassive() {
        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto))
                .isInstanceOf(Exception.class);

        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void updateMeeting_ShouldWork_WhenTeamCardNotPassive() {
        teamCardDto.setPassive(false);

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        meetingService.updateMeeting(meetingId, teamCardId, updateDto);

        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    @Test
    void updateMeeting_ShouldWork_WhenPassiveIsNull() {
        teamCardDto.setPassive(null);

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        meetingService.updateMeeting(meetingId, teamCardId, updateDto);

        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }
}