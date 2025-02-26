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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(BaseApplicationTest.USERNAME)
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
            .build(),
        Meeting.builder()
            .link("https://example.com/meeting")
            .number("12345")
            .startDate(OffsetDateTime.now().plusDays(1))
            .teamCard(teamCard)
            .status(MeetingStatus.OK)
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
        .andExpect(jsonPath("$.teamCardId").value(teamCard.getId().toString()));
  }

  @Test
  void createMeeting_teamCardNotFound() throws Exception {

    var meetingCreateDto = MeetingCreateDto.builder()
        .link("https://example.com/meeting")
        .number("12345")
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
}