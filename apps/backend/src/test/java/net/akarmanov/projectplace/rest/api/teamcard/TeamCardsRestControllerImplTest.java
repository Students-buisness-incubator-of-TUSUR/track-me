package net.akarmanov.projectplace.rest.api.teamcard;

import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.ReadinessLevel;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.models.TeamCardStatus;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import net.akarmanov.projectplace.repos.TeamCardsRepository;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.Year;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamCardsRestControllerImplTest extends BaseApplicationTest {

    @Autowired
    private TeamCardsService teamCardsService;

    @Autowired
    private TeamCardsRepository teamCardsRepository;

    @Autowired
    private NtiMarketRepository ntiMarketRepository;

    private NTIMarket ntiMarket;

    @BeforeEach
    void setUpNti() {
        ntiMarket = ntiMarketRepository.findAll().getFirst();
    }

    @AfterEach
    void tearDown() {
        teamCardsRepository.deleteAll();
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void createTeamCard_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Test")))
                .andExpect(jsonPath("$.description", is("Test description")))
                .andExpect(jsonPath("$.readinessLevel", is("0-2")))
                .andExpect(jsonPath("$.status", is(TeamCardStatus.OK.name())));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void createTeamCard_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/team-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void updateTeamCard_success() throws Exception {
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .ntiMarket(ntiMarket)
                .name("Team card1")
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(patch("/api/v1/team-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("teamCardId", teamCard.getId().toString())
                        .content("""
                                {
                                  "name": "Updated name",
                                  "description": "Updated description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "3-5"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
                .andExpect(jsonPath("$.name", is("Updated name")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.readinessLevel", is("3-5")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCard_success() throws Exception {
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
                .andExpect(jsonPath("$.name", is("Team card1")))
                .andExpect(jsonPath("$.readinessLevel", is("0-2")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withoutFilters_success() throws Exception {
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card1 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": []
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_likeName_success() throws Exception {
        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .ntiMarket(ntiMarket)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .ntiMarket(ntiMarket)
                .description("Team card2 description")
                .username(BaseApplicationTest.USER)
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "name",
                                      "value": "card1",
                                      "type": "LIKE"
                                    },
                                    {
                                      "fieldName": "readinessLevel",
                                      "value": "0-2",
                                      "type": "EQ"
                                    }
                                  ]
                                }"""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(teamCard1.getId().toString())))
                .andExpect(jsonPath("$.content[0].name", is("Team card1")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_emptyResult() throws Exception {
        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "name",
                                      "value": "card1",
                                      "type": "LIKE"
                                    }
                                  ]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_in_success() throws Exception {
        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .ntiMarket(ntiMarket)
                .name("Team card1")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .description("Team card1 description")
                .build());
        var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .param("sort", "name,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "name",
                                      "values": ["%s", "%s"],
                                      "type": "EQ"
                                    }
                                  ]
                                }
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
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_join_success() throws Exception {
        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card1 description")
                .build());
        var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .param("sort", "name,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "username",
                                      "value": "%s",
                                      "type": "EQ"
                                    }
                                  ]
                                }
                                """.formatted(BaseApplicationTest.USER))
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
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withSomeFilters_success() throws Exception {
        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .ntiMarket(ntiMarket)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .username(BaseApplicationTest.USER)
                .name("Team card2")
                .ntiMarket(ntiMarket)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                  {
                                    "fieldName": "name",
                                    "value": "card1",
                                    "type": "LIKE"
                                  },
                                  {
                                    "fieldName": "status",
                                    "value": "OK",
                                    "type": "EQ"
                                  },
                                  {
                                    "fieldName": "readinessLevel",
                                    "value": "0-2",
                                    "type": "EQ"
                                  }
                                ]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(teamCard1.getId().toString())))
                .andExpect(jsonPath("$.content[0].name", is("Team card1")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_withInvalidField() throws Exception {
        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "invalidField",
                                      "value": "card1",
                                      "type": "LIKE"
                                    }
                                  ]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_withNtiMarketId_success() throws Exception {
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .description("Team card2 description")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "ntiMarket.name",
                                      "value": "%s",
                                      "type": "EQ"
                                    },
                                    {
                                      "fieldName": "name",
                                      "value": "card2",
                                      "type": "LIKE"
                                    }
                                  ]
                                }
                                """.formatted(ntiMarket.getName()))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_withTelegranId_success() throws Exception {
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .description("Team card2 description")
                .ntiMarket(ntiMarket)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "username",
                                      "value": "%s",
                                      "type": "EQ"
                                    },
                                    {
                                      "fieldName": "name",
                                      "value": "card2",
                                      "type": "LIKE"
                                    }
                                  ]
                                }
                                """.formatted(BaseApplicationTest.USER)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_withStreamsName_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Test")))
                .andExpect(jsonPath("$.description", is("Test description")))
                .andExpect(jsonPath("$.readinessLevel", is("0-2")))
                .andExpect(jsonPath("$.status", is(TeamCardStatus.OK.name())));

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "streams.name",
                                      "value": "stream 1",
                                      "type": "EQ"
                                    },
                                    {
                                      "fieldName": "name",
                                      "value": "Test",
                                      "type": "LIKE"
                                    }
                                  ]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_withStreamsYear_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Test")))
                .andExpect(jsonPath("$.description", is("Test description")))
                .andExpect(jsonPath("$.readinessLevel", is("0-2")))
                .andExpect(jsonPath("$.status", is(TeamCardStatus.OK.name())));

        mockMvc.perform(post("/api/v1/team-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "streams.year",
                                      "value": "%s",
                                      "type": "EQ"
                                    }
                                  ]
                                }
                                """.formatted(Year.now().getValue())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void createTeamCard_withInactiveStream() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        stream.setEndDate(LocalDate.now().minusDays(1));
        streamRepository.save(stream);

        mockMvc.perform(post("/api/v1/team-card")
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCardCount_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        mockMvc.perform(post("/api/v1/team-card")
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/team-card/count")
                        .param("streamId", stream.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCard_forbidden() throws Exception {
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarket(ntiMarket)
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString())
                        .with(user("otherUser").roles("TRACKER")))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void createTeamCardForOtherUser_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/admin/team-card")
                        .param("streamId", stream.getId().toString())
                        .param("username", "otherUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketId": "%s",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId()))
                        .with(user(BaseApplicationTest.USER).roles("SUPER_ADMIN")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Test")))
                .andExpect(jsonPath("$.description", is("Test description")))
                .andExpect(jsonPath("$.readinessLevel", is("0-2")))
                .andExpect(jsonPath("$.status", is(TeamCardStatus.OK.name())));

        mockMvc.perform(post("/api/v1/team-cards")
                        .param("id", "Test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "username",
                                      "value": "otherUser",
                                      "type": "EQ"
                                    }
                                  ]
                                }
                                """)
                        .with(user("otherUser").roles("TRACKER")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("Test")));
    }
}