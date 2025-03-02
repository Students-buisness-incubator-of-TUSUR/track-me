package net.akarmanov.projectplace.rest.api.admin;

import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.ReadinessLevel;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.models.TeamCardStatus;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import net.akarmanov.projectplace.repos.TeamCardsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
class TeamCardsAdminRestControllerTest extends BaseApplicationTest {

  @Autowired
  private TeamCardsRepository teamCardsRepository;

  @Autowired
  private NtiMarketRepository ntiMarketRepository;


  @Test
  void getTeamCard_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);
    var testUser = userRepository.findByTelegramId("test_user").orElseThrow();
    var teamCard = teamCardsRepository.save(TeamCard.builder()
        .name("test1")
        .description("test1")
        .status(TeamCardStatus.OK)
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .ntiMarket(ntiMarket)
        .user(testUser)
        .build());


    mockMvc.perform(get("/api/v1/admin/team-card")
            .param("userId", testUser.getId().toString())
            .param("id", teamCard.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(teamCard.getId().toString()));
  }

  @Test
  void getTeamCards_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);
    var testUser = userRepository.findByTelegramId("test_user").orElseThrow();
    var teamCard = teamCardsRepository.save(TeamCard.builder()
        .name("test1")
        .description("test1")
        .status(TeamCardStatus.OK)
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .ntiMarket(ntiMarket)
        .user(testUser)
        .build());

    mockMvc.perform(post("/api/v1/admin/team-cards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "filters": [
                    {
                      "fieldName": "user.telegramId",
                      "type": "EQ",
                      "value": "test_user"
                    }
                  ]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content[0].id").value(teamCard.getId().toString()));
  }

  @Test
  void createTeamCard_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);
    var testUser = userRepository.findByTelegramId("test_user").orElseThrow();

    mockMvc.perform(post("/api/v1/admin/team-card")
            .contentType(MediaType.APPLICATION_JSON)
            .param("userId", testUser.getId().toString())
            .content("""
                {
                  "name": "test1",
                  "description": "test1",
                  "readinessLevel": "0-2",
                  "ntiMarketId": "%s"
                }
                """.formatted(ntiMarket.getId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("test1"))
        .andExpect(jsonPath("$.description").value("test1"));
  }

  @Test
  void updateTeamCard_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);
    var testUser = userRepository.findByTelegramId("test_user").orElseThrow();
    var teamCard = teamCardsRepository.save(TeamCard.builder()
        .name("test1")
        .description("test1")
        .status(TeamCardStatus.OK)
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .ntiMarket(ntiMarket)
        .user(testUser)
        .build());

    mockMvc.perform(post("/api/v1/admin/team-card")
            .contentType(MediaType.APPLICATION_JSON)
            .param("userId", testUser.getId().toString())
            .param("id", teamCard.getId().toString())
            .content("""
                {
                  "name": "test2",
                  "description": "test2",
                  "status": "OK",
                  "readinessLevel": "0-2",
                  "ntiMarketId": "%s"
                }
                """.formatted(ntiMarket.getId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("test2"))
        .andExpect(jsonPath("$.description").value("test2"));
  }

  @Test
  void updateTeamCard_withStream_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);
    var testUser = userRepository.findByTelegramId("test_user").orElseThrow();
    var teamCard = teamCardsRepository.save(TeamCard.builder()
        .name("test1")
        .description("test1")
        .status(TeamCardStatus.OK)
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .ntiMarket(ntiMarket)
        .user(testUser)
        .build());

    mockMvc.perform(post("/api/v1/admin/team-card")
            .contentType(MediaType.APPLICATION_JSON)
            .param("userId", testUser.getId().toString())
            .param("id", teamCard.getId().toString())
            .param("streamId", streamRepository.findAll().get(0).getId().toString())
            .content("""
                {
                  "name": "test2",
                  "description": "test2",
                  "status": "OK",
                  "readinessLevel": "0-2",
                  "ntiMarketId": "%s"
                }
                """.formatted(ntiMarket.getId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("test2"))
        .andExpect(jsonPath("$.description").value("test2"));
  }
}