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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCardSummaryService {
    /**
     * Низкий рейтинг.
     */
    private static final double LOW_GRADE = 0.25;

    /**
     * Сервис карточек команд.
     */
    private final TeamCardsService teamCardsService;

    /**
     * Репозиторий карточек команд.
     */
    private final TeamCardsRepository teamCardsRepository;

    /**
     * Поставщик сообщений карточек команд.
     */
    private final TeamCardEventsProducer teamCardEventsProducer;

    /**
     * Сообщить о командах с низким рейтингом.
     */
    @Transactional
    @Scheduled(cron = "0 2 5 * * 1")
    public void reportAboutTeamCardLowGrades() {
        log.info("Scheduled team card low grade summary started");
        sendTeamCardLowGradeSummary();
        log.info("Scheduled team card low grade summary completed");
    }

    /**
     * Отправить сводку о командах с пропущенными встречами.
     * @param meetingSummaryEvents Событие пропущенных встреч
     */
    public void sendTeamCardsSummary(
            List<LinkedHashMap<String, String>> meetingSummaryEvents) {
        List<TeamCardSummaryEvent> teamCardSummaryEvents = new ArrayList<>();

        for (var meetingSummaryEvent : meetingSummaryEvents) {
            var teamCard = teamCardsService
                    .getTeamCard(UUID.fromString(meetingSummaryEvent.get("teamCardId")));

            getStreamByTeamCard(teamCard).ifPresentOrElse(stream -> {
                TeamCardSummaryEvent teamCardSummaryEvent =
                        TeamCardSummaryEvent.builder()
                                .teamCardName(teamCard.getName())
                                .streamName(stream.getName())
                                .meetingNumber(meetingSummaryEvent.get("meetingNumber"))
                                .meetingLink(meetingSummaryEvent.get("meetingLink"))
                                .build();

                teamCardSummaryEvents.add(teamCardSummaryEvent);
            }, () -> log.info("Team card {} has no active streams.", teamCard.getId()));
        }

        if (teamCardSummaryEvents.isEmpty()) {
            return;
        }

         teamCardEventsProducer.sendTeamCardSummaryEvent(teamCardSummaryEvents);
    }

    private void sendTeamCardLowGradeSummary() {
        List<TeamCardLowGradeSummaryEvent> teamCardLowGradeSummaryEvents =
                new ArrayList<>();

        var teamCards = teamCardsRepository.findAll().stream()
                .filter(teamCard -> teamCard.getAverageGrade()
                        .compareTo(BigDecimal.valueOf(LOW_GRADE)) <= 0)
                .toList();

        if (teamCards.isEmpty()) {
            log.info("No team cards with low grade");
            return;
        }

        for (var teamCard : teamCards) {
            getStreamByTeamCard(teamCard).ifPresentOrElse(stream -> {
                TeamCardLowGradeSummaryEvent event = TeamCardLowGradeSummaryEvent.builder()
                        .teamCardName(teamCard.getName())
                        .streamName(stream.getName())
                        .averageGrade(teamCard.getAverageGrade())
                        .build();

                teamCardLowGradeSummaryEvents.add(event);
            }, () -> log.info("Team card {} has no active streams.", teamCard.getId()));
        }

        if (teamCardLowGradeSummaryEvents.isEmpty()) {
            return;
        }

        teamCardEventsProducer.sendTeamCardLowGradeSummaryEvent(teamCardLowGradeSummaryEvents);
    }

    private Optional<Stream> getStreamByTeamCard(TeamCard teamCard) {
        return teamCard.getStreams().stream().filter(Stream::isActive).findFirst();
    }
}
