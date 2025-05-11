package net.akarmanov.projectplace.rest.api.meeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.domain.ReadinessLevel;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.models.MeetingStatus;
import net.akarmanov.projectplace.repos.MeetingRepository;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import net.akarmanov.projectplace.repos.TeamCardsRepository;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(value = BaseApplicationTest.USER, roles = {"TRACKER"})
class MeetingRestControllerTest extends BaseApplicationTest {

    @Autowired
    private TeamCardsService teamCardsService;

    @Autowired
    private TeamCardsRepository teamCardsRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private NtiMarketRepository ntiMarketRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private TeamCard teamCard;

    @BeforeEach
    void setUp() {
        teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .name("Test")
                .description("Test")
                .username(BaseApplicationTest.USER)
                .ntiMarket(ntiMarketRepository.findAll().getFirst())
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        meetingRepository.saveAll(List.of(
                Meeting.builder()
                        .link("https://example.com/meeting")
                        .number("12343")
                        .startDate(OffsetDateTime.now().plusDays(1))
                        .teamCard(teamCard)
                        .status(MeetingStatus.OK)
                        .tasksCurrentMeeting("tasksCurrentMeeting")
                        .tasksNextMeeting("tasksNextMeeting")
                        .build(),
                Meeting.builder()
                        .link("https://example.com/meeting")
                        .number("12345")
                        .startDate(OffsetDateTime.now().plusDays(1))
                        .teamCard(teamCard)
                        .status(MeetingStatus.OK)
                        .tasksCurrentMeeting("tasksCurrentMeeting")
                        .tasksNextMeeting("tasksNextMeeting")
                        .build()
        ));
    }

    @AfterEach
    void tearDown() {
        meetingRepository.deleteAll();
        teamCardsRepository.deleteAll();
    }

    @Test
    void createMeeting_success() throws Exception {

        var meetingCreateDto = MeetingCreateDto.builder()
                .link("https://example.com/meeting")
                .number("12345")
                .status(MeetingStatus.OK)
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/meetings")
                        .param("teamCardId", teamCard.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(meetingCreateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.link").value(meetingCreateDto.link()))
                .andExpect(jsonPath("$.number").value(meetingCreateDto.number()))
                .andExpect(jsonPath("$.status").value(meetingCreateDto.status().toString()))
                .andExpect(jsonPath("$.teamCardId").value(teamCard.getId().toString()));
    }

    @Test
    void createMeeting_teamCardNotFound() throws Exception {

        var meetingCreateDto = MeetingCreateDto.builder()
                .link("https://example.com/meeting")
                .number("12345")
                .status(MeetingStatus.OK)
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/meetings")
                        .param("teamCardId", "00000000-0000-0000-0000-000000000000")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(meetingCreateDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void getMeetings_success() throws Exception {
        mockMvc.perform(get("/api/v1/meetings")
                        .param("teamCardId", teamCard.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = {"SUPER_ADMIN"})
    void getMeetings_success_superAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/meetings")
                        .param("teamCardId", teamCard.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void updateMeeting_success() throws Exception {
        var meetingUpdateDto = MeetingUpdateDto.builder()
                .link("https://example.com/meeting")
                .number("12345")
                .status(MeetingStatus.MANY_ISSUES)
                .build();

        var teamCardId = teamCard.getId().toString();
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        mockMvc.perform(patch("/api/v1/meetings/" + meetingId)
                        .param("teamCardId", teamCardId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(meetingUpdateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.link").value("https://example.com/meeting"))
                .andExpect(jsonPath("$.number").value("12345"));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = {"SUPER_ADMIN"})
    void updateMeeting_bySuperAdmin_success() throws Exception {
        var meetingUpdateDto = MeetingUpdateDto.builder()
                .link("https://example.com/meeting")
                .number("123456")
                .status(MeetingStatus.OK)
                .build();

        var teamCardId = teamCard.getId().toString();
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        mockMvc.perform(patch("/api/v1/meetings/" + meetingId)
                        .param("teamCardId", teamCardId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(meetingUpdateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.link").value("https://example.com/meeting"))
                .andExpect(jsonPath("$.number").value("123456"));
    }
}