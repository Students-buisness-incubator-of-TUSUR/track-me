package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private AclService aclService;

    @Mock
    private MeetingEventsProducer meetingEventsProducer;

    @Mock
    private BackendApiClient userBackendApiClient;

    @Mock
    private SsoApiClient ssoApiClient;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private UUID activeTeamId;
    private UUID passiveTeamId;
    private TeamCardDto activeTeamCard;
    private TeamCardDto passiveTeamCard;

    @BeforeEach
    void setUp() {
        activeTeamId = UUID.randomUUID();
        passiveTeamId = UUID.randomUUID();
        UUID streamId = UUID.randomUUID();

        activeTeamCard = TeamCardDto.builder()
                .id(activeTeamId)
                .name("Active Team")
                .username("tracker_user")
                .passive(false)
                .streams(List.of(new StreamDto(streamId)))
                .meetingRoomLink("https://zoom.us/j/active")
                .build();

        passiveTeamCard = TeamCardDto.builder()
                .id(passiveTeamId)
                .name("Passive Team")
                .username("tracker_user")
                .passive(true)
                .streams(List.of(new StreamDto(streamId)))
                .meetingRoomLink("https://zoom.us/j/passive")
                .build();

        UserDto mockTracker = UserDto.builder()
                .id(UUID.randomUUID().toString())
                .username("tracker_user")
                .fullName("Иван Трекеров")
                .build();

        when(ssoApiClient.getTrackers()).thenReturn(List.of(mockTracker));
    }

    @Test
    void createMeetingForActiveTeamTrackerAllowed() {
        when(userBackendApiClient.getTeamCardById(activeTeamId)).thenReturn(activeTeamCard);

        Meeting mockMeeting = new Meeting();
        mockMeeting.setId(UUID.randomUUID());

        when(meetingMapper.mapToEntity(any(MeetingCreateDto.class))).thenReturn(mockMeeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(mockMeeting);

        MeetingCreateDto dto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .tasksCurrentMeeting("Task")
                .tasksNextMeeting("Next")
                .build();

        var result = meetingService.createMeeting(activeTeamId, dto);
        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(activeTeamId.toString());
    }

    @Test
    void createMeetingForPassiveTeamTrackerThrowsException() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        MeetingCreateDto dto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> meetingService.createMeeting(passiveTeamId, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может создавать встречи для пассивной команды");
    }

    @Test
    void createMeetingForPassiveTeamAdminAllowed() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        Meeting mockMeeting = new Meeting();
        mockMeeting.setId(UUID.randomUUID());

        when(meetingMapper.mapToEntity(any(MeetingCreateDto.class))).thenReturn(mockMeeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(mockMeeting);

        MeetingCreateDto dto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        var result = meetingService.createMeeting(passiveTeamId, dto);
        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(passiveTeamId.toString());
    }

    @Test
    void updateMeetingForPassiveTeamTrackerThrowsException() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        UUID meetingId = UUID.randomUUID();
        MeetingUpdateDto dto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .build();

        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, passiveTeamId, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может редактировать встречи пассивной команды");
    }

    @Test
    void updateMeetingForPassiveTeamAdminAllowed() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        UUID meetingId = UUID.randomUUID();
        Meeting existingMeeting = new Meeting();
        existingMeeting.setId(meetingId);
        existingMeeting.setStatus(MeetingStatus.SCHEDULED);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(existingMeeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(existingMeeting);

        MeetingUpdateDto dto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .recordLink("https://updated.com")
                .build();

        var result = meetingService.updateMeeting(meetingId, passiveTeamId, dto);
        assertThat(result).isNotNull();
        assertThat(result.teamStatus()).isEqualTo(TeamStatus.OK);
    }

}