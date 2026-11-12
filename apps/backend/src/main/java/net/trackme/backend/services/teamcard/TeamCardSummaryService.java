package net.trackme.backend.services.teamcard;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.services.exceptions.TeamCardNotFoundException;
import net.trackme.backend.messaging.TeamCardLowGradeSummaryEvent;
import net.trackme.backend.messaging.TeamCardSummaryEvent;
import net.trackme.backend.repos.TeamCardsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Количество команд на странице сводки.
     */
    private static final int SUMMARY_PAGE_SIZE = 10;

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
     * Контекст загруженных сущностей, освобождаемый после каждой страницы.
     */
    private final EntityManager entityManager;

    /**
     * Сообщить о командах с низким рейтингом.
     */
    @Transactional(readOnly = true)
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
            List<Map<String, String>> meetingSummaryEvents) {
        List<TeamCardSummaryEvent> teamCardSummaryEvents = new ArrayList<>();

        for (var meetingSummaryEvent : meetingSummaryEvents) {
            var teamCardId = UUID.fromString(meetingSummaryEvent.get("teamCardId"));

            try {
                var teamCard = teamCardsService.getTeamCard(teamCardId);
                if (Boolean.TRUE.equals(teamCard.getPassive())) {
                    continue;
                }

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
        var pageRequest = PageRequest.of(0, SUMMARY_PAGE_SIZE, Sort.by("id"));
        List<TeamCardLowGradeSummaryEvent> summaryEvents = new ArrayList<>();
        Slice<TeamCard> teamCards;

        do {
            teamCards = teamCardsRepository.findAllBy(pageRequest);
            summaryEvents.addAll(buildTeamCardLowGradeSummaryEvents(teamCards.getContent()));
            entityManager.clear();
            pageRequest = pageRequest.next();
        } while (teamCards.hasNext());

        log.info("Total events to send: {}", summaryEvents.size());

        if (summaryEvents.isEmpty()) {
            log.info("No events to send");
            return;
        }

        teamCardEventsProducer.sendTeamCardLowGradeSummaryEvent(summaryEvents);
    }

    /**
     * Формирует строки общей сводки для одной страницы, не отправляя отдельное письмо.
     *
     * @param batch страница команд
     * @return строки сводки без ссылок на загруженные сущности
     */
    private List<TeamCardLowGradeSummaryEvent> buildTeamCardLowGradeSummaryEvents(List<TeamCard> batch) {
        List<TeamCardLowGradeSummaryEvent> teamCardLowGradeSummaryEvents = new ArrayList<>();

        var teamCards = batch.stream()
                .filter(teamCard -> !Boolean.TRUE.equals(teamCard.getPassive()))
                .filter(teamCard -> teamCard.getAverageGrade()
                        .compareTo(BigDecimal.valueOf(LOW_GRADE)) <= 0)
                .toList();

        log.info("Found {} team cards with low grade", teamCards.size());

        if (teamCards.isEmpty()) {
            log.info("No team cards with low grade");
            return List.of();
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

        return teamCardLowGradeSummaryEvents;
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
