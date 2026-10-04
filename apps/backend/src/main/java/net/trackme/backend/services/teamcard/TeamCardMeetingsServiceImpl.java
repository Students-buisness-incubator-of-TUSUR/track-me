package net.trackme.backend.services.teamcard;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.MeetingGrade;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.messaging.MeetingNotHappenedEvent;
import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.MeetingGradeRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.exceptions.TeamCardNotFoundException;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCardMeetingsServiceImpl implements TeamCardMeetingsService {

    /**
     * Репозиторий карточек команд.
     */
    private final TeamCardsRepository teamCardsRepository;

    /**
     * Репозиторий встреч команд.
     */
    private final MeetingGradeRepository meetingGradeRepository;

    /**
     * Поставщик сообщений о карточках команд.
     */
    private final TeamCardEventsProducer teamCardEventsProducer;

    @Override
    @Transactional
    public void increaseMeetingCount(UUID teamCardId, UUID meetingId) {
        teamCardsRepository.findById(teamCardId)
                .filter(teamCard -> teamCard.getMeetingGrades().stream()
                        .noneMatch(grade -> grade.getMeetingId().equals(meetingId)))
                .ifPresentOrElse(
                        teamCard -> {
                            teamCard.increaseMeetingCount();
                            teamCard.addMeetingGrade(meetingId);
                            teamCardsRepository.saveAndFlush(teamCard);
                            calculateAverageGrade(teamCard);
                            log.info("Increased meeting count for team card {}. Current count: {}. Average grade: {}",
                                    teamCardId, teamCard.getMeetingsCount(), teamCard.getAverageGrade());
                        },
                        () -> log.warn("Team card {} not found or meeting {} already exists", teamCardId, meetingId));
    }

    @Override
    @Transactional
    public void updateTeamCardInfo(UUID teamCardId,
                                UUID meetingId, MeetingStatus newStatus,
                                MeetingStatus oldStatus,
                                TeamCardStatus teamCardStatus,
                                BigDecimal teamGrade,
                                String meetingLink) {
        // Обновляем grade в MeetingGrade
        meetingGradeRepository.findByMeetingIdAndTeamCardId(meetingId, teamCardId)
                .ifPresent(meetingGrade -> {
                    if (teamGrade != null) {
                        meetingGrade.setGrade(teamGrade);
                        meetingGradeRepository.saveAndFlush(meetingGrade);
                    }
                });

        teamCardsRepository.findById(teamCardId)
                .ifPresentOrElse(
                        teamCard -> {
                            // ВАЖНО: Обновляем статус counters только если статус реально изменился
                            updateStatusCounters(teamCard, oldStatus, newStatus, meetingLink);

                            if (teamCardStatus != null) {
                                teamCard.setStatus(teamCardStatus);
                            }

                            // Пересчитываем средний рейтинг ВСЕГДА, если grade изменился
                            if (teamGrade != null) {
                                calculateAverageGrade(teamCard);
                            }

                            teamCardsRepository.saveAndFlush(teamCard);
                            log.info("Updated team card {}: completed={}, completedAsNotHappened={}, avgGrade={}",
                                    teamCardId,
                                    teamCard.getMeetingsCompletedCount(),
                                    teamCard.getMeetingsCompletedAsNotHappenedCount(),
                                    teamCard.getAverageGrade());
                        },
                        () -> log.warn("Team card {} not found", teamCardId));
    }

    /**
     * Обновляет счётчики статусов.
     * ВАЖНО: При изменении статуса НЕ увеличиваем meetingCount,
     * только пересчитываем счётчики completed/completedAsNotHappened.
     */
    private void updateStatusCounters(TeamCard teamCard, MeetingStatus oldStatus,
                                    MeetingStatus newStatus, String meetingLink) {
        if (oldStatus == newStatus) {
            // Статус не изменился - ничего не делаем со счётчиками
            if (newStatus == MeetingStatus.SCHEDULED) {
                sendMeetingNotHappenedEvent(teamCard, meetingLink);
            }
            return;
        }

        // Уменьшаем счётчик старого статуса
        decrementCounter(teamCard, oldStatus);
        // Увеличиваем счётчик нового статуса
        incrementCounter(teamCard, newStatus);
    }

    private void decrementCounter(TeamCard teamCard, MeetingStatus status) {
        if (status == MeetingStatus.COMPLETED) {
            teamCard.setMeetingsCompletedCount(
                Math.max(0, teamCard.getMeetingsCompletedCount() - 1));
        } else if (status == MeetingStatus.COMPLETED_AS_NOT_HAPPENED) {
            teamCard.setMeetingsCompletedAsNotHappenedCount(
                Math.max(0, teamCard.getMeetingsCompletedAsNotHappenedCount() - 1));
        }
    }

    private void incrementCounter(TeamCard teamCard, MeetingStatus status) {
        if (status == MeetingStatus.COMPLETED) {
            teamCard.setMeetingsCompletedCount(
                teamCard.getMeetingsCompletedCount() + 1);
        } else if (status == MeetingStatus.COMPLETED_AS_NOT_HAPPENED) {
            teamCard.setMeetingsCompletedAsNotHappenedCount(
                teamCard.getMeetingsCompletedAsNotHappenedCount() + 1);
        }
    }

    @Override
    @Transactional
    public void handleMeetingDeleted(UUID teamCardId, UUID meetingId, MeetingStatus status) {
        var teamCard = teamCardsRepository.findById(teamCardId)
                .orElseThrow(() -> new TeamCardNotFoundException(teamCardId));

        // Ищем MeetingGrade через репозиторий
        var meetingGradeOpt = meetingGradeRepository.findByMeetingIdAndTeamCardId(meetingId, teamCardId);

        if (meetingGradeOpt.isEmpty()) {
            log.warn("Meeting grade for meeting {} not found in team card {}, skipping",
                    meetingId, teamCardId);
            return;
        }

        var meetingGrade = meetingGradeOpt.get();

        // Удаляем из коллекции TeamCard
        teamCard.getMeetingGrades().remove(meetingGrade);

        // Удаляем из репозитория
        meetingGradeRepository.delete(meetingGrade);
        meetingGradeRepository.flush();

        // Уменьшаем общий счётчик встреч
        teamCard.setMeetingsCount(Math.max(0, teamCard.getMeetingsCount() - 1));

        // Уменьшаем счётчик соответствующего статуса
        decrementCounter(teamCard, status);

        // Пересчитываем средний рейтинг
        calculateAverageGrade(teamCard);

        // Сохраняем изменения в TeamCard
        teamCardsRepository.saveAndFlush(teamCard);

        log.info("Team card {} updated after meeting {} deletion. Status: {}, meetingsCount: {}, completedCount: {}, notHappenedCount: {}, avgGrade: {}",
                teamCardId, meetingId, status,
                teamCard.getMeetingsCount(),
                teamCard.getMeetingsCompletedCount(),
                teamCard.getMeetingsCompletedAsNotHappenedCount(),
                teamCard.getAverageGrade());
    }

    private void calculateAverageGrade(TeamCard teamCard) {
        var grades = teamCard.getMeetingGrades().stream()
                .map(MeetingGrade::getGrade)
                .filter(Objects::nonNull)
                .toList();

        if (grades.isEmpty()) {
            teamCard.setAverageGrade(BigDecimal.ZERO);
        } else {
            var total = grades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            var average = total.divide(BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);
            teamCard.setAverageGrade(average);
        }
    }

    private void sendMeetingNotHappenedEvent(TeamCard teamCard, String meetingLink) {
        teamCard.getStreams().stream()
                .filter(Stream::isActive)
                .findFirst()
                .ifPresentOrElse(stream -> {
                    var event = MeetingNotHappenedEvent.builder()
                            .teamCardUsername(teamCard.getUsername())
                            .teamCardName(teamCard.getName())
                            .streamName(stream.getName())
                            .meetingLink(meetingLink)
                            .build();
                    teamCardEventsProducer.sendMeetingNotHappenedEvent(event);
                    }, () -> log.info("Team card {} has no active streams.", teamCard.getId()));
    }
}
