package net.trackme.backend.rest.api.admin;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamCardsAdminRestControllerTest extends BaseApplicationTest {
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
    @WithMockUser(value = BaseApplicationTest.USER, roles = "SUPER_ADMIN")
    void testUpdateTeamCardOwner() throws Exception {
        var teamCard = teamCardsService.createTeamCard(
                TeamCard.builder()
                        .status(TeamCardStatus.OK)
                        .ntiMarkets(List.of(ntiMarket))
                        .name("Team card1")
                        .meetingRoomLink("meetingRoom@link.com")
                        .readinessLevel(ReadinessLevel.LEVEL_1)
                        .build(),
                "tracker1");

        mockMvc.perform(get("/api/v1/team-card")
                        .with(csrf())
                        .param("id", teamCard.getId().toString())
                        .with(user("tracker1").roles("TRACKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())));

        mockMvc.perform(patch("/api/v1/admin/team-card")
                        .param("teamCardId", teamCard.getId().toString())
                        .param("username", "tracker2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ntiMarketIds": ["%s"],
                                  "name": "Team card2",
                                  "readinessLevel": "0-2"
                                }
                                """.formatted(ntiMarket.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
                .andExpect(jsonPath("$.username", is("tracker2")));

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString())
                        .with(user("tracker2").roles("TRACKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())));

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString())
                        .with(user("tracker1").roles("TRACKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "SUPER_ADMIN")
    void getTeamCard_success() throws Exception {
        var teamCard = teamCardsService.createTeamCard(
                TeamCard.builder()
                        .status(TeamCardStatus.OK)
                        .ntiMarkets(List.of(ntiMarket))
                        .name("Team card1")
                        .meetingRoomLink("meetingRoom@link.com")
                        .readinessLevel(ReadinessLevel.LEVEL_1)
                        .build(),
                "tracker1");

        mockMvc.perform(get("/api/v1/admin/team-card")
                        .param("id", teamCard.getId().toString())
                        .param("username", teamCard.getUsername())
                        .with(user(BaseApplicationTest.USER).roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())))
                .andExpect(jsonPath("$.username", is(teamCard.getUsername())));
    }


    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "SUPER_ADMIN")
    void getTeamCards_success() throws Exception {
        teamCardsService.createTeamCard(
                TeamCard.builder()
                        .status(TeamCardStatus.OK)
                        .ntiMarkets(List.of(ntiMarket))
                        .name("Team card 1")
                        .meetingRoomLink("meetingRoom@link.com")
                        .readinessLevel(ReadinessLevel.LEVEL_1)
                        .build(),
                BaseApplicationTest.USER);

        teamCardsService.createTeamCard(
                TeamCard.builder()
                        .status(TeamCardStatus.OK)
                        .ntiMarkets(List.of(ntiMarket))
                        .name("Team card 2")
                        .meetingRoomLink("meetingRoom@link.com")
                        .readinessLevel(ReadinessLevel.LEVEL_1)
                        .build(),
                BaseApplicationTest.USER);

        mockMvc.perform(post("/api/v1/admin/team-cards")
                        .with(csrf())
                        .param("sort", "name,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": []
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Team card 1"))
                .andExpect(jsonPath("$.content[1].name").value("Team card 2"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER,
            roles = "SUPER_ADMIN")
    void deleteTeamCard_success() throws Exception {
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Team card1")
                .meetingRoomLink("meetingRoom@link.com")
                .ntiMarkets(List.of(ntiMarket))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());

        mockMvc.perform(delete("/api/v1/admin/team-card")
                        .with(csrf())
                        .param("id", teamCard.getId().toString())
                        .param("username", teamCard.getUsername())
                        .with(user(BaseApplicationTest.USER).roles("SUPER_ADMIN")))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}