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
import net.trackme.meetingservice.api.dto.MeetingDto;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки поведения MeetingServiceImpl при работе с пассивными командами.
 * Проверяет, что трекер не может создавать встречи для пассивной команды.
 */
@ExtendWith(MockitoExtension.class)
class MeetingServicePassiveTest {

    /**
     * Репозиторий встреч.
     */
    @Mock
    private MeetingRepository meetingRepository;

    /**
     * Маппер для преобразования DTO в сущности.
     */
    @Mock
    private MeetingMapper meetingMapper;

    /**
     * Сервис для работы с ACL.
     */
    @Mock
    private AclService aclService;

    /**
     * Продюсер событий встреч.
     */
    @Mock
    private MeetingEventsProducer meetingEventsProducer;

    /**
     * Клиент для взаимодействия с backend API.
     */
    @Mock
    private BackendApiClient userBackendApiClient;

    /**
     * Клиент для взаимодействия с SSO API.
     */
    @Mock
    private SsoApiClient ssoApiClient;

    /**
     * Тестируемый сервис встреч.
     */
    @InjectMocks
    private MeetingServiceImpl meetingService;

    /**
     * Идентификатор пассивной команды.
     */
    private UUID passiveTeamId;

    /**
     * DTO пассивной команды.
     */
    private TeamCardDto passiveTeamCard;

    @BeforeEach
    void setUp() {
        passiveTeamId = UUID.randomUUID();
        passiveTeamCard = TeamCardDto.builder()
                .id(passiveTeamId)
                .name("Passive Team")
                .username("tracker")
                .passive(true)
                .streams(List.of(new StreamDto(UUID.randomUUID())))
                .meetingRoomLink("https://zoom.us/j/123")
                .build();
    }

    @Test
    @WithMockUser(username = "tracker", roles = "TRACKER")
    void createMeetingForPassiveTeamTrackerShouldThrowException() {
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
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMeetingForPassiveTeamAdminShouldNotThrowException() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        MeetingCreateDto dto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        Meeting mockMeeting = new Meeting();
        mockMeeting.setId(UUID.randomUUID());
        when(meetingMapper.mapToEntity(any(MeetingCreateDto.class))).thenReturn(mockMeeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(mockMeeting);

        MeetingDto result = meetingService.createMeeting(passiveTeamId, dto);

        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(passiveTeamId.toString());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMeetingForPassiveTeamAdminShouldNotThrowException() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        UUID meetingId = UUID.randomUUID();
        Meeting existingMeeting = new Meeting();
        existingMeeting.setId(meetingId);
        existingMeeting.setStatus(MeetingStatus.SCHEDULED);
        existingMeeting.setTeamCardId(passiveTeamId);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(existingMeeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(existingMeeting);

        MeetingUpdateDto dto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .build();

        MeetingDto result = meetingService.updateMeeting(meetingId, passiveTeamId, dto);

        assertThat(result).isNotNull();
        assertThat(result.teamStatus()).isEqualTo(TeamStatus.OK);
    }

}
