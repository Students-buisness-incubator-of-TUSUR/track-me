package net.trackme.backend.rest.api.teamcard;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @AfterEach
    void tearDown() {
        teamCardsRepository.deleteAll();
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void createTeamCard_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        var ntiMarkets = ntiMarketRepository.findAll().stream()
                .map(NTIMarket::getId)
                .map(UUID::toString)
                .toList();

        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(String.join("\", \"", ntiMarkets))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Test")))
                .andExpect(jsonPath("$.description", is("Test description")))
                .andExpect(jsonPath("$.readinessLevel", is("0-2")))
                .andExpect(jsonPath("$.streams[0].id", is(stream.getId().toString())))
                .andExpect(jsonPath("$.status", is(TeamCardStatus.OK.name())));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void createTeamCard_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void updateTeamCard_success() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .ntiMarkets(List.of(ntiMarket))
                .name("Team card1")
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(patch("/api/v1/team-card")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("teamCardId", teamCard.getId().toString())
                        .content("""
                                {
                                  "name": "Updated name",
                                  "description": "Updated description",
                                  "ntiMarketIds": ["%s"],
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarkets(List.of(ntiMarket))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
                .andExpect(jsonPath("$.name", is("Team card1")))
                .andExpect(jsonPath("$.averageGrade").isNumber())
                .andExpect(jsonPath("$.readinessLevel", is("0-2")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withoutFilters_success() throws Exception {
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card1 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
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
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .ntiMarkets(List.of(ntiMarket1))
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .ntiMarkets(List.of(ntiMarket2))
                .description("Team card2 description")
                .username(BaseApplicationTest.USER)
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
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
                        .with(csrf())
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_in_success() throws Exception {
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .ntiMarkets(List.of(ntiMarket1))
                .name("Team card1")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .description("Team card1 description")
                .build());
        var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
                        .param("sort", "name,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "name",
                                      "values": ["%s", "%s"],
                                      "type": "IN"
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
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card1 description")
                .build());
        var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
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
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        var teamCard1 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .ntiMarkets(List.of(ntiMarket1))
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .username(BaseApplicationTest.USER)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card2 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
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
                        .with(csrf())
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
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .description("Team card2 description")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "ntiMarkets.name",
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
                                """.formatted(ntiMarket2.getName()))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "TRACKER")
    void getTeamCards_withFilters_withTelegranId_success() throws Exception {
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .description("Team card1 description")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .description("Team card2 description")
                .ntiMarkets(List.of(ntiMarket2))
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .username(BaseApplicationTest.USER)
                .build());

        mockMvc.perform(post("/api/v1/team-cards")
                        .with(csrf())
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
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
                        .with(csrf())
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "streams.year",
                                      "value": "%s",
                                      "type": "EQ"
                                    },
                                    {
                                      "fieldName": "enabled",
                                      "value": true,
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarkets(List.of(ntiMarket))
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
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/admin/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .param("username", "otherUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
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
                        .with(csrf())
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