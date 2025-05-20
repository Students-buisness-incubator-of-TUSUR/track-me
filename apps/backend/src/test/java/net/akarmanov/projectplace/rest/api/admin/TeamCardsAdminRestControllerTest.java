package net.akarmanov.projectplace.rest.api.admin;

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

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                        .ntiMarket(ntiMarket)
                        .name("Team card1")
                        .readinessLevel(ReadinessLevel.LEVEL_1)
                        .build(),
                "tracker1");

        mockMvc.perform(get("/api/v1/team-card")
                        .param("id", teamCard.getId().toString())
                        .with(user("tracker1").roles("TRACKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(teamCard.getId().toString())));

        mockMvc.perform(patch("/api/v1/admin/team-card")
                        .param("teamCardId", teamCard.getId().toString())
                        .param("username", "tracker2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ntiMarketId": "%s",
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
}