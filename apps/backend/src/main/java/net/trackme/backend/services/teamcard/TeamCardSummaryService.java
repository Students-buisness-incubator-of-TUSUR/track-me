package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.services.exceptions.TeamCardNotFoundException;
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

/**
 * Сервис для формирования сводок по карточкам команд.
 */
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
    @Scheduled(cron = "0 0 9 * * 1", zone = "Asia/Tomsk")
    public void reportAboutTeamCardLowGrades() {
        log.info("Scheduled team card low grade summary started");
        sendTeamCardLowGradeSummary();
        log.info("Scheduled team card low grade summary completed");
    }

    /**
     * Отправить сводку о командах с пропущенными встречами.
     *
     * @param meetingSummaryEvents Событие пропущенных встреч
     */
    public void sendTeamCardsSummary(
            List<LinkedHashMap<String, String>> meetingSummaryEvents) {
        List<TeamCardSummaryEvent> teamCardSummaryEvents = new ArrayList<>();

        for (var meetingSummaryEvent : meetingSummaryEvents) {
            var teamCardId = UUID.fromString(meetingSummaryEvent.get("teamCardId"));

            try {
                var teamCard = teamCardsService.getTeamCard(teamCardId);
                getStreamByTeamCard(teamCard).ifPresentOrElse(stream -> {
                    String trackerFullName = meetingSummaryEvent.getOrDefault(
                            "trackerFullName", "Не назначен");

                    TeamCardSummaryEvent teamCardSummaryEvent =
                            TeamCardSummaryEvent.builder()
                                    .teamCardName(teamCard.getName())
                                    .streamName(stream.getName())
                                    .meetingNumber(meetingSummaryEvent.get("meetingNumber"))
                                    .meetingLink(meetingSummaryEvent.get("meetingLink"))
                                    .trackerFullName(trackerFullName)
                                    .build();

                    teamCardSummaryEvents.add(teamCardSummaryEvent);
                }, () -> log.info("Team card {} has no active streams.", teamCard.getId()));
            } catch (TeamCardNotFoundException e) {
                log.warn("Team card {} not found, skipping: {}", teamCardId, e.getMessage());
            }
        }

        if (teamCardSummaryEvents.isEmpty()) {
            log.info("No team card summary events to send");
            return;
        }

        teamCardEventsProducer.sendTeamCardSummaryEvent(teamCardSummaryEvents);
    }

    /**
     * Отправляет сводку о командах с низким рейтингом.
     */
    private void sendTeamCardLowGradeSummary() {
        List<TeamCardLowGradeSummaryEvent> teamCardLowGradeSummaryEvents = new ArrayList<>();

        var teamCards = teamCardsRepository.findAll().stream()
                .filter(teamCard -> teamCard.getAverageGrade()
                        .compareTo(BigDecimal.valueOf(LOW_GRADE)) <= 0)
                .toList();

        log.info("Found {} team cards with low grade", teamCards.size());

        if (teamCards.isEmpty()) {
            log.info("No team cards with low grade");
            return;
        }

        for (var teamCard : teamCards) {
            log.info("Processing teamCard: name={}, avgGrade={}, trackerFullName={}",
                teamCard.getName(), teamCard.getAverageGrade(), teamCard.getTrackerFullName());

            getStreamByTeamCard(teamCard).ifPresentOrElse(stream -> {
                String trackerFullName = teamCard.getTrackerFullName() != null
                        ? teamCard.getTrackerFullName()
                        : "Не назначен";

                log.info("Adding to summary: stream={}, team={}, tracker={}, grade={}",
                    stream.getName(), teamCard.getName(), trackerFullName,
                    teamCard.getAverageGrade());

                TeamCardLowGradeSummaryEvent event = TeamCardLowGradeSummaryEvent.builder()
                        .teamCardName(teamCard.getName())
                        .streamName(stream.getName())
                        .averageGrade(teamCard.getAverageGrade())
                        .trackerFullName(trackerFullName)
                        .build();

                teamCardLowGradeSummaryEvents.add(event);
            }, () -> log.info("Team card {} has no active streams.", teamCard.getId()));
        }

        log.info("Total events to send: {}", teamCardLowGradeSummaryEvents.size());

        if (teamCardLowGradeSummaryEvents.isEmpty()) {
            log.info("No events to send");
            return;
        }

        teamCardEventsProducer.sendTeamCardLowGradeSummaryEvent(teamCardLowGradeSummaryEvents);
    }

    /**
     * Получает активный поток для карточки команды.
     *
     * @param teamCard карточка команды
     * @return Optional с активным потоком или пустой Optional
     */
    private Optional<Stream> getStreamByTeamCard(TeamCard teamCard) {
        return teamCard.getStreams().stream().filter(Stream::isActive).findFirst();
    }
}
