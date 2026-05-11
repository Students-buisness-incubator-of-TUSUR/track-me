package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.entities.Meeting;
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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
                .passive(true)  // 👈 пассивный статус
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
    void createMeeting_forActiveTeam_tracker_allowed() {
        // Arrange
        when(userBackendApiClient.getTeamCardById(activeTeamId)).thenReturn(activeTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .tasksCurrentMeeting("Test task")
                .tasksNextMeeting("Next task")
                .build();

        // Act & Assert - не должно быть исключения
        var result = meetingService.createMeeting(activeTeamId, createDto);
        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(activeTeamId.toString());
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void createMeeting_forPassiveTeam_tracker_throwsException() {
        // Arrange
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> meetingService.createMeeting(passiveTeamId, createDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может создавать встречи для пассивной команды");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createMeeting_forPassiveTeam_admin_allowed() {
        // Arrange
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        // Act & Assert - админ может создавать встречи для пассивной команды
        var result = meetingService.createMeeting(passiveTeamId, createDto);
        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(passiveTeamId.toString());
    }

    @Test
    @WithMockUser(username = "tracker_user", roles = "TRACKER")
    void updateMeeting_forPassiveTeam_tracker_throwsException() {
        // Arrange
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        // Сначала создаём встречу через админа (у нас нет другого способа)
        // В реальном тесте нужно создать встречу через репозиторий
        var meetingId = UUID.randomUUID();

        var updateDto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, passiveTeamId, updateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Трекер не может редактировать встречи пассивной команды");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateMeeting_forPassiveTeam_admin_allowed() {
        // Arrange
        when(userBackendApiClient.getTeamCardById(passiveTeamId)).thenReturn(passiveTeamCard);

        // Создаём встречу
        var createDto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        var createdMeeting = meetingService.createMeeting(passiveTeamId, createDto);

        var updateDto = MeetingUpdateDto.builder()
                .teamStatus(TeamStatus.OK)
                .recordLink("https://updated.com")
                .build();

        // Act & Assert - админ может редактировать
        var result = meetingService.updateMeeting(createdMeeting.id(), passiveTeamId, updateDto);
        assertThat(result).isNotNull();
        assertThat(result.teamStatus()).isEqualTo(TeamStatus.OK);
    }
}