package net.trackme.meetingservice.api;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingTeamIdTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeetingRepository meetingRepository;

    @MockitoBean(name = "userBackendApiClient")
    private BackendApiClient userBackendApiClient;

    @MockitoBean
    private SsoApiClient ssoApiClient;

    private UUID teamCardId;
    private UUID streamId;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();

        teamCardId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        streamId = UUID.randomUUID();

        var mockTeamCard = TeamCardDto.builder()
                .id(teamCardId)
                .name("Test Team")
                .username("tracker_user")
                .streams(List.of(new StreamDto(streamId)))
                .meetingRoomLink("https://zoom.us/j/123")
                .build();

        var mockTracker = UserDto.builder()
                .id(UUID.randomUUID().toString())
                .username("tracker_user")
                .fullName("Иван Трекеров")
                .build();

        when(userBackendApiClient.getTeamCardById(teamCardId)).thenReturn(mockTeamCard);
        when(ssoApiClient.getTrackers()).thenReturn(List.of(mockTracker));
    }

    @Test
    @WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
    void testMeetingReportContainsTeamId() throws Exception {
        // Создаем встречу с teamCardId
        Meeting meeting = Meeting.builder()
                .teamCardId(teamCardId)
                .teamName("Test Team Name")
                .trackerUsername("tracker_user")
                .trackerFullName("Иван Трекеров")
                .status(MeetingStatus.COMPLETED)
                .teamStatus(TeamStatus.OK)
                .startDate(OffsetDateTime.now())
                .build();
        meetingRepository.save(meeting);

        // Вызываем API отчета
        mockMvc.perform(post("/api/v1/meetings/reports")
                        .param("streamId", streamId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\":[]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teamId").value(teamCardId.toString()))
                .andExpect(jsonPath("$.content[0].teamName").value("Test Team Name"));
    }

    @Test
    @WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
    void testMeetingReportTeamIdIsPresent() throws Exception {
        // Создаем встречу с teamCardId
        Meeting meeting = Meeting.builder()
                .teamCardId(teamCardId)
                .teamName("Another Team")
                .trackerUsername("tracker_user")
                .trackerFullName("Иван Трекеров")
                .status(MeetingStatus.COMPLETED)
                .teamStatus(TeamStatus.WITH_ISSUES)
                .startDate(OffsetDateTime.now())
                .build();
        meetingRepository.save(meeting);

        // Вызываем API отчета и проверяем наличие teamId
        mockMvc.perform(post("/api/v1/meetings/reports")
                        .param("streamId", streamId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\":[]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teamId").exists())
                .andExpect(jsonPath("$.content[0].teamId").isNotEmpty());
    }
}