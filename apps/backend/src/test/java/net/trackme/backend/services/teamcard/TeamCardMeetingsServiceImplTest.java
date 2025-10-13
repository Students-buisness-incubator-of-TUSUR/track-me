package net.trackme.backend.services.teamcard;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

class TeamCardMeetingsServiceImplTest extends BaseApplicationTest {
    @Autowired
    private TeamCardsService teamCardsService;

    @Autowired
    private TeamCardsRepository teamCardsRepository;

    @Autowired
    private TeamCardMeetingsServiceImpl teamCardMeetingsService;

    @Autowired
    private NtiMarketRepository ntiMarketRepository;

    @AfterEach
    void tearDown() {
        teamCardsRepository.deleteAll();
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "TRACKER")
    void increaseMeetingCount() {
        // Arrange
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Test team card")
                .ntiMarkets(List.of(ntiMarket))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());
        var meetingId = UUID.randomUUID();

        // Act
        teamCardMeetingsService.increaseMeetingCount(teamCard.getId(), meetingId);

        // Assert
        var expectedTeamCard = teamCardsService.getTeamCard(teamCard.getId());
        Assertions.assertEquals(teamCard.getMeetingsCount() + 1, expectedTeamCard.getMeetingsCount());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "TRACKER")
    void updateTeamCardInfo() {
        // Arrange
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Test team card")
                .ntiMarkets(List.of(ntiMarket))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());
        var meetingId = UUID.randomUUID();

        // Act
        teamCardMeetingsService.updateTeamCardInfo(
                teamCard.getId(),
                meetingId,
                MeetingStatus.COMPLETED,
                MeetingStatus.SCHEDULED,
                teamCard.getStatus(),
                BigDecimal.ZERO);

        // Assert
        var expectedTeamCard = teamCardsService.getTeamCard(teamCard.getId());
        Assertions.assertEquals(teamCard.getMeetingsCompletedCount() + 1, expectedTeamCard.getMeetingsCompletedCount());
    }
}