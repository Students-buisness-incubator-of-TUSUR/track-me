package net.trackme.backend.rest.api.teamcard;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                                  "meetingRoomLink": "https://test.link",
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
                .meetingRoomLink("meetingRoom@link.com")
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
                                  "readinessLevel": "3-5",
                                  "meetingRoomLink": "https://new.link"
                                }
                                """.formatted(ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
                .andExpect(jsonPath("$.name", is("Updated name")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.readinessLevel", is("3-5")))
                .andExpect(jsonPath("$.meetingRoomLink", is("https://new.link")));
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
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .description("Team card1 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .username(BaseApplicationTest.USER)
                .ntiMarkets(List.of(ntiMarket1))
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .username(BaseApplicationTest.USER)
                .description("Team card1 description")
                .build());
        var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .description("Team card1 description")
                .build());
        var teamCard2 = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .username(BaseApplicationTest.USER)
                .ntiMarkets(List.of(ntiMarket1))
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .username(BaseApplicationTest.USER)
                .name("Team card2")
                .ntiMarkets(List.of(ntiMarket2))
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .description("Team card2 description")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("meetingRoom@link.com")
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
                .meetingRoomLink("meetingRoom@link.com")
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card2")
                .description("Team card2 description")
                .ntiMarkets(List.of(ntiMarket2))
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("meetingRoom@link.com")
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
                                  "readinessLevel": "0-2",
                                  "meetingRoomLink": "https://test.link"
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
                                  "meetingRoomLink": "https://test.link",
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
                                  "meetingRoomLink": "https://test.link",
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
                                  "readinessLevel": "0-2",
                                  "meetingRoomLink": "https://test.link"
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
                .meetingRoomLink("meetingRoom@link.com")
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
                                  "readinessLevel": "0-2",
                                  "meetingRoomLink": "https://test.link"
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

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "TRACKER")
    void deleteTeamCard_success() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .ntiMarkets(List.of(ntiMarket))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("meetingRoom@link.com")
                .build());

        mockMvc.perform(delete("/api/v1/team-card")
                        .with(csrf())
                        .param("id", teamCard.getId().toString())
                        .with(user(BaseApplicationTest.USER).roles("TRACKER")))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_withoutFilters_success() throws Exception {
        var stream1 = streamRepository.findAll().getFirst();
        var stream2 = streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());
        var stream3 = streamRepository.save(Stream.builder()
                .name("stream 3")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().minusDays(2))
                .build());
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 1")
                .ntiMarkets(List.of(ntiMarket1))
                .meetingRoomLink("meetingRoom@link.com")
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .description("Team card 1 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 2")
                .streams(Set.of(stream1))
                .meetingRoomLink("meetingRoom@link.com")
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card 2 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 3")
                .streams(Set.of(stream2))
                .ntiMarkets(List.of(ntiMarket2))
                .meetingRoomLink("meetingRoom@link.com")
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card 3 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 4")
                .streams(Set.of(stream3))
                .ntiMarkets(List.of(ntiMarket2))
                .meetingRoomLink("meetingRoom@link.com")
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .description("Team card 4 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": []
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(3)))
                .andExpect(jsonPath("$.content[?(@.streamName=='stream 3')].startDate",
                        hasItem(stream3.getStartDate().toString())))
                .andExpect(jsonPath("$.content[?(@.streamName=='stream 3')].endDate",
                        hasItem(stream3.getEndDate().toString())));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_withFilters_success() throws Exception {
        var stream1 = streamRepository.findAll().getFirst();
        var stream2 = streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());
        var stream3 = streamRepository.save(Stream.builder()
                .name("stream 3")
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().minusDays(2))
                .build());
        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 1")
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("meetingRoom@link.com")
                .description("Team card 1 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 2")
                .streams(Set.of(stream1))
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("meetingRoom@link.com")
                .description("Team card 2 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 3")
                .streams(Set.of(stream2))
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("meetingRoom@link.com")
                .description("Team card 3 description")
                .build());
        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 4")
                .streams(Set.of(stream3))
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("meetingRoom@link.com")
                .description("Team card 4 description")
                .build());

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "streams.name",
                                      "value": "%s",
                                      "type": "EQ"
                                    }
                                  ]
                                }
                                """.formatted(stream1.getName())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].streamName", is(stream1.getName())));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_pagination_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        saveTeamCardWithStream("tracker1", "Team A", 5.0, stream);
        saveTeamCardWithStream("tracker1", "Team B", 4.0, stream);
        saveTeamCardWithStream("tracker1", "Team C", 3.0, stream);
        saveTeamCardWithStream("tracker1", "Team D", 2.0, stream);
        saveTeamCardWithStream("tracker1", "Team E", 1.0, stream);

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements", is(5)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("Team A")))
                .andExpect(jsonPath("$.content[1].teamCardName", is("Team B")));

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "name,asc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("Team C")))
                .andExpect(jsonPath("$.content[1].teamCardName", is("Team D")));


        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .param("page", "2")
                        .param("size", "2")
                        .param("sort", "name,asc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("Team E")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_complexPaginationAndSort_success() throws Exception {
        var stream1 = streamRepository.save(Stream.builder()
                .name("Alpha")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build()
        );

        var stream2 = streamRepository.save(Stream.builder()
                .name("Beta")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build()
        );

        saveTeamCardWithStream("tr1", "Team 1", 5.0, stream1);
        saveTeamCardWithStream("tr1", "Team 2", 3.0, stream1);
        saveTeamCardWithStream("tr2", "Team 3", 4.5, stream1);
        saveTeamCardWithStream("tr2", "Team 4", 1.0, stream1);
        saveTeamCardWithStream("tr1", "Other", 4.8, stream2);

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "averageGrade,desc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "filters": [
                            {"fieldName": "streams.name", "value": "Alpha", "type": "EQ"}
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements", is(4)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("Team 1")))
                .andExpect(jsonPath("$.content[1].teamCardName", is("Team 3")))
                .andExpect(jsonPath("$.content[*].teamCardName", not(hasItem("Other"))));

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "averageGrade,desc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "filters": [
                            {"fieldName": "streams.name", "value": "Alpha", "type": "EQ"}
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("Team 2")))
                .andExpect(jsonPath("$.content[1].teamCardName", is("Team 4")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_shouldExcludeCardsWithoutStreams() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        saveTeamCardWithStream("tracker1", "With Stream", 5.0, stream);

        teamCardsService.createTeamCard(TeamCard.builder()
                .username("tracker1")
                .name("Without Stream")
                .enabled(true)
                .meetingRoomLink("https://link.com")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("With Stream")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_shouldReturnCorrectAggregatedGrades() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        saveTeamCardWithStream("tracker1", "Team 1", 3.0, stream);
        saveTeamCardWithStream("tracker1", "Team 2", 5.0, stream);

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username=='tracker1')].averageUserGrade", everyItem(is(4.0))));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_withComplexFilter_success() throws Exception {
        var stream1 = streamRepository.save(Stream.builder()
                .name("Alpha")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build()
        );

        var stream2 = streamRepository.save(Stream.builder()
                .name("Beta")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build()
        );

        saveTeamCardWithStream("tr1", "TargetTeam", 5.0, stream1);
        saveTeamCardWithStream("tr1", "OtherTeam", 5.0, stream2);

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "filters": [
                            {"fieldName": "name", "value": "Target", "type": "LIKE"},
                            {"fieldName": "streams.name", "value": "Alpha", "type": "EQ"}
                          ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].teamCardName", is("TargetTeam")));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReportExcel_withoutFilters_success() throws Exception {
        var stream1 = streamRepository.findAll().getFirst();
        var stream2 = streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());

        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 1")
                .streams(Set.of(stream1))
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .meetingRoomLink("https://test-link.com")
                .description("Team card 1 description")
                .build());

        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 2")
                .streams(Set.of(stream2))
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .meetingRoomLink("https://test-link.com")
                .description("Team card 2 description")
                .build());

        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/team-cards/reports/excel")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"filters\": []}")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatchResult = mockMvc
                .perform(asyncDispatch(mvcResult))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("spreadsheetml.sheet")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename")))
                .andReturn();


        byte[] responseBytes = dispatchResult.getResponse().getContentAsByteArray();
        assertThat(responseBytes).isNotEmpty();

        try (var opcPackage = OPCPackage.open(new ByteArrayInputStream(responseBytes));
             var workbook = new XSSFWorkbook(opcPackage)) {
            assertThat(workbook.getNumberOfSheets()).isGreaterThan(0);
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isGreaterThan(1);
        }
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReportExcel_withFilters_success() throws Exception {
        var stream1 = streamRepository.findAll().getFirst();
        var stream2 = streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());

        var ntiMarket1 = ntiMarketRepository.findAll().get(0);
        var ntiMarket2 = ntiMarketRepository.findAll().get(1);

        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 1")
                .streams(Set.of(stream1))
                .ntiMarkets(List.of(ntiMarket1))
                .username(BaseApplicationTest.USER)
                .meetingRoomLink("https://test-link.com")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card 2")
                .streams(Set.of(stream2))
                .ntiMarkets(List.of(ntiMarket2))
                .username(BaseApplicationTest.USER)
                .meetingRoomLink("https://test-link.com")
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .build());

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/team-cards/reports/excel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "filters": [
                        {
                          "fieldName": "streams.name",
                          "value": "%s",
                          "type": "EQ"
                        }
                      ]
                    }
                    """.formatted(stream1.getName())))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatchResult = mockMvc
                .perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        byte[] responseBytes = dispatchResult.getResponse().getContentAsByteArray();
        assertThat(responseBytes).isNotEmpty();

        try (var opcPackage = OPCPackage.open(new ByteArrayInputStream(responseBytes));
             var workbook = new XSSFWorkbook(opcPackage)) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCardsReport_shouldReturnCorrectMeetingsCountPlan() throws Exception {
        var now = LocalDate.now();
        var streamEndDate = now.plusDays(60);

        // Arrange - создаем поток с явно заданным meetingsCount
        var streamWithMeetingsCount2 = streamRepository.save(Stream.builder()
                .name("Stream with meetingsCount 2")
                .startDate(now.minusDays(30))
                .endDate(streamEndDate)
                .trackStartDate(now.minusDays(8))
                .meetingsCount(2)  // 👈 ЯВНО УСТАНАВЛИВАЕМ ЗНАЧЕНИЕ
                .build()
        );

        var streamWithMeetingsCount1 = streamRepository.save(Stream.builder()
                .name("Stream with meetingsCount 1")
                .startDate(now)
                .endDate(streamEndDate)
                .trackStartDate(now)
                .meetingsCount(1)  // 👈 ЯВНО УСТАНАВЛИВАЕМ ЗНАЧЕНИЕ
                .build()
        );

        saveTeamCardWithStream("tracker1", "Team With Plan 2", 4.0, streamWithMeetingsCount2);
        saveTeamCardWithStream("tracker2", "Team With Plan 1", 4.0, streamWithMeetingsCount1);

        // Act & Assert - проверяем, что возвращается установленное значение meetingsCount
        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "streams.name",
                                      "value": "Stream with meetingsCount 2",
                                      "type": "EQ"
                                    }
                                  ]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].meetingsCountPlan", is(2)));  // Ожидаем 2

        mockMvc.perform(post("/api/v1/team-cards/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "streams.name",
                                      "value": "Stream with meetingsCount 1",
                                      "type": "EQ"
                                    }
                                  ]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].meetingsCountPlan", is(1)));  // Ожидаем 1
    }

    private void saveTeamCardWithStream(String username, String name, double grade, Stream stream) {
        teamCardsService.createTeamCard(TeamCard.builder()
                .username(username)
                .name(name)
                .status(TeamCardStatus.OK)
                .enabled(true)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of())
                .meetingRoomLink("https://link.com")
                .averageGrade(BigDecimal.valueOf(grade))
                .streams(Set.of(stream))
                .build());
    }


    // ==================== ПРОСТЫЕ ТЕСТЫ ДЛЯ ПАССИВНОГО СТАТУСА ====================

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void createTeamCard_defaultPassiveIsFalse() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "Test Passive Default",
                                  "description": "Test description",
                                  "ntiMarketIds": ["%s"],
                                  "meetingRoomLink": "https://test.link",
                                  "readinessLevel": "0-2"
                                }
                                """, ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passive").value(false));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void updateTeamCard_setPassiveToTrue() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        // Создаем команду
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Test Passive Set")
                .username(BaseApplicationTest.USER)
                .meetingRoomLink("https://test.link")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket))
                .build());

        // Обновляем - устанавливаем passive = true
        mockMvc.perform(patch("/api/v1/team-card")
                        .with(csrf())
                        .param("teamCardId", teamCard.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "meetingRoomLink": "https://test.link",
                                  "ntiMarketIds": ["%s"],
                                  "readinessLevel": "3-5",
                                  "passive": true
                                }
                                """, teamCard.getName(), ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passive").value(true));
    }


    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void updateTeamCard_withPassiveTeam_byAdmin_shouldSucceed() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        // Создаем пассивную команду
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Passive Team")
                .username("someUser")
                .meetingRoomLink("https://test.link")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket))
                .passive(true)
                .build());

        // Админ обновляет пассивную команду
        mockMvc.perform(patch("/api/v1/team-card")
                        .with(csrf())
                        .param("teamCardId", teamCard.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "Updated By Admin",
                                  "meetingRoomLink": "https://test.link",
                                  "ntiMarketIds": ["%s"],
                                  "readinessLevel": "3-5"
                                }
                                """, ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated By Admin"));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void getTeamCard_shouldReturnPassiveFlag() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team With Passive")
                .username(BaseApplicationTest.USER)
                .meetingRoomLink("https://test.link")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket))
                .passive(true)
                .build());

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passive").value(true));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void updateTeamCard_setPassiveFromTrueToFalse() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team To Deactivate")
                .username(BaseApplicationTest.USER)
                .meetingRoomLink("https://test.link")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket))
                .passive(true)
                .build());

        // Меняем passive с true на false
        mockMvc.perform(patch("/api/v1/team-card")
                        .with(csrf())
                        .param("teamCardId", teamCard.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "meetingRoomLink": "https://test.link",
                                  "ntiMarketIds": ["%s"],
                                  "readinessLevel": "3-5",
                                  "passive": false
                                }
                                """, teamCard.getName(), ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passive").value(false));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
    void updateTeamCard_setPassiveFromFalseToTrue() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team To Activate")
                .username(BaseApplicationTest.USER)
                .meetingRoomLink("https://test.link")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket))
                .passive(false)
                .build());

        // Меняем passive с false на true
        mockMvc.perform(patch("/api/v1/team-card")
                        .with(csrf())
                        .param("teamCardId", teamCard.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "meetingRoomLink": "https://test.link",
                                  "ntiMarketIds": ["%s"],
                                  "readinessLevel": "3-5",
                                  "passive": true
                                }
                                """, teamCard.getName(), ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passive").value(true));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "SUPER_ADMIN")
    void updateTeamCard_withPassiveTeam_bySuperAdmin_shouldSucceed() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Passive Team")
                .username("someUser")
                .meetingRoomLink("https://test.link")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .ntiMarkets(List.of(ntiMarket))
                .passive(true)
                .build());

        mockMvc.perform(patch("/api/v1/team-card")
                        .with(csrf())
                        .param("teamCardId", teamCard.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "Updated By SuperAdmin",
                                  "meetingRoomLink": "https://test.link",
                                  "ntiMarketIds": ["%s"],
                                  "readinessLevel": "3-5"
                                }
                                """, ntiMarket.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated By SuperAdmin"));
    }

}


