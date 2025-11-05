package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.messaging.TeamCardLowGradeSummaryEvent;
import net.trackme.backend.messaging.TeamCardSummaryEvent;
import net.trackme.backend.repos.TeamCardsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCardSummaryService {

    private final TeamCardsService teamCardsService;

    private final TeamCardsRepository teamCardsRepository;

    private final TeamCardEventsProducer teamCardEventsProducer;

    @Transactional
    @Scheduled(cron = "* 0 12 * * 7")
    public void reportAboutTeamCardLowGrades() {
        log.info("Scheduled team card low grade summary started");
        sendTeamCardLowGradeSummary();
        log.info("Scheduled team card low grade summary completed");
    }

    public void sendTeamCardsSummary(
            List<LinkedHashMap<String, String>> meetingSummaryEvents) {
        List<TeamCardSummaryEvent> teamCardSummaryEvents = new ArrayList<>();

        for (var meetingSummaryEvent : meetingSummaryEvents) {
            var teamCard = teamCardsService
                    .getTeamCard(UUID.fromString(meetingSummaryEvent.get("teamCardId")));

            var stream = getStreamByTeamCard(teamCard);

            TeamCardSummaryEvent teamCardSummaryEvent =
                    TeamCardSummaryEvent.builder()
                            .teamCardName(teamCard.getName())
                            .streamName(stream.getName())
                            .meetingNumber(meetingSummaryEvent.get("meetingNumber"))
                            .meetingLink(meetingSummaryEvent.get("meetingLink"))
                            .build();

            teamCardSummaryEvents.add(teamCardSummaryEvent);
        }

         teamCardEventsProducer.sendTeamCardSummaryEvent(teamCardSummaryEvents);
    }

    private void sendTeamCardLowGradeSummary(){
        List<TeamCardLowGradeSummaryEvent> teamCardLowGradeSummaryEvents =
                new ArrayList<>();

        var teamCards = teamCardsRepository.findAll().stream()
                .filter(teamCard -> teamCard.getAverageGrade()
                        .compareTo(BigDecimal.valueOf(0.25)) < 0)
                .toList();

        if (teamCards.isEmpty()){
            log.info("No team cards with low grade");
            return;
        }

        for (var teamCard : teamCards) {
            var stream = getStreamByTeamCard(teamCard);

            TeamCardLowGradeSummaryEvent event = TeamCardLowGradeSummaryEvent.builder()
                    .teamCardName(teamCard.getName())
                    .streamName(stream.getName())
                    .averageGrade(teamCard.getAverageGrade())
                    .build();

            teamCardLowGradeSummaryEvents.add(event);
        }

        teamCardEventsProducer.sendTeamCardLowGradeSummaryEvent(teamCardLowGradeSummaryEvents);
    }

    private Stream getStreamByTeamCard(TeamCard teamCard){
        return teamCard.getStreams().stream()
                .filter(Stream::isActive)
                .findFirst().orElseThrow();
    }
}
