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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamCardsRestControllerImplTest extends BaseApplicationTest {

  private static final UsernamePasswordAuthenticationToken superadmin = new UsernamePasswordAuthenticationToken(
      "superadmin", "superadmin");

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
}