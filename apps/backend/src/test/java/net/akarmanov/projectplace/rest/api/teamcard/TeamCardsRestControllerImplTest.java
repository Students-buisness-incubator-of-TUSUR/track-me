package net.akarmanov.projectplace.rest.api.teamcard;

import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.models.TeamCardStatus;
import net.akarmanov.projectplace.repos.TeamCardsRepository;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamCardsRestControllerImplTest extends BaseApplicationTest {

  @Autowired
  private TeamCardsService teamCardsService;

  @Autowired
  private TeamCardsRepository teamCardsRepository;

  @AfterEach
  void tearDown() {
    teamCardsRepository.deleteAll();
  }

  @Test
  @WithMockUser(value = "test_tracker")
  void createTeamCard_success() throws Exception {
    mockMvc.perform(post("/api/v1/team-card")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Test",
                  "description": "Test description"
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name", is("Test")));
  }

  @Test
  @WithMockUser("test_tracker")
  void createTeamCard_validationError() throws Exception {
    mockMvc.perform(post("/api/v1/team-card")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser("test_tracker")
  void updateTeamCard_success() throws Exception {
    var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .build());

    mockMvc.perform(patch("/api/v1/team-card")
            .contentType(MediaType.APPLICATION_JSON)
            .param("teamCardId", teamCard.getId().toString())
            .content("""
                {
                  "name": "Updated name",
                  "description": "Updated description"
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
        .andExpect(jsonPath("$.name", is("Updated name")));
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCard_success() throws Exception {
    var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .build());

    mockMvc.perform(get("/api/v1/team-card")
            .param("id", teamCard.getId().toString()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
        .andExpect(jsonPath("$.name", is("Team card1")));
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withoutFilters_success() throws Exception {
    teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .description("Team card1 description")
        .build());
    teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card2")
        .description("Team card2 description")
        .build());

    mockMvc.perform(post("/api/v1/team-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[]"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(2)));
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withFilters_likeName_success() throws Exception {
    var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .description("Team card1 description")
        .build());
    teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card2")
        .description("Team card2 description")
        .build());

    mockMvc.perform(post("/api/v1/team-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                [
                  {
                    "fieldName": "name",
                    "value": "card1",
                    "type": "LIKE"
                  }
                ]
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id", is(teamCard1.getId().toString())))
        .andExpect(jsonPath("$.content[0].name", is("Team card1")))
        .andExpect(jsonPath("$.page.totalElements", is(1)));
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withFilters_emptyResult() throws Exception {
    mockMvc.perform(post("/api/v1/team-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                [
                  {
                    "fieldName": "name",
                    "value": "card1",
                    "type": "LIKE"
                  }
                ]
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withFilters_validationError() throws Exception {
    mockMvc.perform(post("/api/v1/team-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withFilters_in_success() throws Exception {
    var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .description("Team card1 description")
        .build());
    var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card2")
        .description("Team card2 description")
        .build());

    mockMvc.perform(post("/api/v1/team-cards")
            .param("sort", "name,asc")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                [
                  {
                    "fieldName": "name",
                    "values": ["%s", "%s"],
                    "type": "EQ"
                  }
                ]
                """.formatted(teamCard1.getName(), teamCard2.getName()))
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id", is(teamCard1.getId().toString())))
        .andExpect(jsonPath("$.content[0].name", is("Team card1")))
        .andExpect(jsonPath("$.content[1].id", is(teamCard2.getId().toString())))
        .andExpect(jsonPath("$.content[1].name", is("Team card2")))
        .andExpect(jsonPath("$.page.totalElements", is(2)));
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withFilters_join_success() throws Exception {
    var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .description("Team card1 description")
        .build());
    var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card2")
        .description("Team card2 description")
        .build());

    mockMvc.perform(post("/api/v1/team-cards")
            .param("sort", "name,asc")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                [
                  {
                    "fieldName": "user.telegramId",
                    "value": "%s",
                    "type": "EQ"
                  }
                ]
                """.formatted("test_tracker"))
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id", is(teamCard1.getId().toString())))
        .andExpect(jsonPath("$.content[0].name", is("Team card1")))
        .andExpect(jsonPath("$.content[1].id", is(teamCard2.getId().toString())))
        .andExpect(jsonPath("$.content[1].name", is("Team card2")))
        .andExpect(jsonPath("$.page.totalElements", is(2)));
  }

  @Test
  @WithMockUser(value = "test_tracker",
                roles = "TRACKER")
  void getTeamCards_withSomeFilters_success() throws Exception {
    var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card1")
        .description("Team card1 description")
        .build());
    teamCardsService.createTeamCard(TeamCard.builder()
        .status(TeamCardStatus.OK)
        .name("Team card2")
        .description("Team card2 description")
        .build());

    mockMvc.perform(post("/api/v1/team-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                [
                  {
                    "fieldName": "name",
                    "value": "card1",
                    "type": "LIKE"
                  },
                  {
                    "fieldName": "status",
                    "value": "OK",
                    "type": "EQ"
                  }
                ]
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id", is(teamCard1.getId().toString())))
        .andExpect(jsonPath("$.content[0].name", is("Team card1")))
        .andExpect(jsonPath("$.page.totalElements", is(1)));
  }
}