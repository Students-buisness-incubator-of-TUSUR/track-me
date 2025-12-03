package net.trackme.backend.services.teamcard;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.StreamRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class TeamCardMeetingsServiceImplTest extends BaseApplicationTest {
    @Autowired
    private TeamCardsService teamCardsService;

    @Autowired
    private TeamCardsRepository teamCardsRepository;

    @Autowired
    private StreamRepository streamRepository;

    @Autowired
    private TeamCardMeetingsServiceImpl teamCardMeetingsService;

    @Autowired
    private NtiMarketRepository ntiMarketRepository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

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
        var meetingId1 = UUID.randomUUID();
        var meetingId2 = UUID.randomUUID();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Test team card")
                .ntiMarkets(List.of(ntiMarket))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());
        teamCard.addMeetingGrade(meetingId1);
        teamCard.addMeetingGrade(meetingId2);
        teamCardsRepository.save(teamCard);
        var meetingLink = "test link";

        // Act
        teamCardMeetingsService.updateTeamCardInfo(
                teamCard.getId(),
                meetingId1,
                MeetingStatus.COMPLETED,
                MeetingStatus.SCHEDULED,
                teamCard.getStatus(),
                BigDecimal.ONE,
                meetingLink);

        // Assert
        var expectedTeamCard = teamCardsService.getTeamCard(teamCard.getId());
        Assertions.assertEquals(
                BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP),
                expectedTeamCard.getAverageGrade());
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "TRACKER")
    void updateTeamCardInfo_meetingNotHappened() {
        // Arrange
        var stream = streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var teamCard = teamCardsService.createTeamCard(TeamCard.builder()
                .status(TeamCardStatus.OK)
                .name("Test team card")
                .ntiMarkets(List.of(ntiMarket))
                .streams(Set.of(stream))
                .username(BaseApplicationTest.USER)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build());
        var meetingId = UUID.randomUUID();
        var meetingLink = "test link";

        // Act
        teamCardMeetingsService.updateTeamCardInfo(
                teamCard.getId(),
                meetingId,
                MeetingStatus.NOT_HAPPENED,
                MeetingStatus.SCHEDULED,
                teamCard.getStatus(),
                BigDecimal.ZERO,
                meetingLink);

        // Assert
        verify(kafkaTemplate).send(any(Message.class));
    }
}