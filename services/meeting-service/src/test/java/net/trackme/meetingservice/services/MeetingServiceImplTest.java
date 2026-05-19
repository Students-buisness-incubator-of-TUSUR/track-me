package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.kafka.bootstrap-servers=",
        "kafka.enabled=false"
})
class MeetingServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private MeetingService meetingService;

    @MockitoBean
    @Qualifier("userBackendApiClient")
    private BackendApiClient userBackendApiClient;

    @MockitoBean
    private SsoApiClient ssoApiClient;

    private UUID activeTeamId;
    private UUID passiveTeamId;
    private TeamCardDto activeTeamCard;
    private TeamCardDto passiveTeamCard;

    @BeforeEach
    void setUp() {
        activeTeamId = UUID.randomUUID();
        passiveTeamId = UUID.randomUUID();

        activeTeamCard = TeamCardDto.builder()
                .id(activeTeamId)
                .name("Active Team")
                .username("tracker_user")
                .passive(false)
                .streams(List.of(new StreamDto(UUID.randomUUID())))
                .meetingRoomLink("https://zoom.us/j/active")
                .build();

        passiveTeamCard = TeamCardDto.builder()
                .id(passiveTeamId)
                .name("Passive Team")
                .username("tracker_user")
                .passive(true)
                .streams(List.of(new StreamDto(UUID.randomUUID())))
                .meetingRoomLink("https://zoom.us/j/passive")
                .build();

        var mockTracker = UserDto.builder()
                .id(UUID.randomUUID().toString())
                .username("tracker_user")
                .fullName("Иван Трекеров")
                .build();

        when(ssoApiClient.getTrackers()).thenReturn(List.of(mockTracker));
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void createMeetingForActiveTeamTrackerAllowed() {
        when(userBackendApiClient.getTeamCardById(activeTeamId)).thenReturn(activeTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .tasksCurrentMeeting("Test task")
                .tasksNextMeeting("Next task")
                .build();

        var result = meetingService.createMeeting(activeTeamId, createDto);
        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(activeTeamId.toString());
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void createMeetingForPassiveTeamTrackerThrowsException() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> meetingService.createMeeting(passiveTeamId, createDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может создавать встречи для пассивной команды");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMeetingForPassiveTeamAdminAllowed() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        var result = meetingService.createMeeting(passiveTeamId, createDto);
        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(passiveTeamId.toString());
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void updateMeetingForPassiveTeamTrackerThrowsException() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        var createdMeeting = meetingService.createMeeting(passiveTeamId, createDto);

        var updateDto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .build();

        assertThatThrownBy(() -> meetingService.updateMeeting(createdMeeting.id(), passiveTeamId, updateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может редактировать встречи пассивной команды");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMeetingForPassiveTeamAdminAllowed() {
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        var createdMeeting = meetingService.createMeeting(passiveTeamId, createDto);

        var updateDto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .recordLink("https://updated.com")
                .build();

        var result = meetingService.updateMeeting(createdMeeting.id(), passiveTeamId, updateDto);
        assertThat(result).isNotNull();
        assertThat(result.teamStatus()).isEqualTo(TeamStatus.OK);
    }
}