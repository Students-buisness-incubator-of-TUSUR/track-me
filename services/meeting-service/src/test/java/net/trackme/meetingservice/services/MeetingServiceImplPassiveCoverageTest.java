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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки доступа к пассивным командам в MeetingServiceImpl.
 * Проверяют, что трекер не может создавать/редактировать встречи для пассивной команды.
 */
@ExtendWith(MockitoExtension.class)
class MeetingServiceImplPassiveCoverageTest {

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
    private BackendApiClient userBackendClient;

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
        UUID streamId = UUID.randomUUID();

        passiveTeamCard = TeamCardDto.builder()
                .id(passiveTeamId)
                .name("Passive Team")
                .username("tracker_user")
                .passive(true)
                .streams(List.of(new StreamDto(streamId)))
                .meetingRoomLink("https://zoom.us/j/passive")
                .build();
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void createMeetingForPassiveTeamTrackerThrowsException() {
        when(userBackendClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        MeetingCreateDto dto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> meetingService.createMeeting(passiveTeamId, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может создавать встречи для пассивной команды");
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void updateMeetingForPassiveTeamTrackerThrowsException() {
        when(userBackendClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        UUID meetingId = UUID.randomUUID();
        Meeting existingMeeting = new Meeting();
        existingMeeting.setId(meetingId);
        existingMeeting.setStatus(MeetingStatus.SCHEDULED);
        existingMeeting.setTeamCardId(passiveTeamId);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(existingMeeting));

        MeetingUpdateDto dto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .build();

        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, passiveTeamId, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может редактировать встречи пассивной команды");
    }
}