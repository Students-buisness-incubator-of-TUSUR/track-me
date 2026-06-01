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
                            log.info(
                                    "Increased meeting count for team card {}. "
                                            + "Current count: {}. Average grade: {}",
                                    teamCardId,
                                    teamCard.getMeetingsCount(),
                                    teamCard.getAverageGrade());
                        },
                        () -> log.warn("Team card {} not found", teamCardId));
    }

    @Override
    @Transactional
    public void updateTeamCardInfo(UUID teamCardId,
                                   UUID meetingId, MeetingStatus newStatus,
                                   MeetingStatus oldStatus,
                                   TeamCardStatus teamCardStatus,
                                   BigDecimal teamGrade,
                                   String meetingLink) {
        meetingGradeRepository.findByMeetingIdAndTeamCardId(meetingId, teamCardId)
                .ifPresent(meetingGrade -> {
                    meetingGrade.setGrade(teamGrade);
                    meetingGradeRepository.saveAndFlush(meetingGrade);
                });

        teamCardsRepository.findById(teamCardId)
                .ifPresentOrElse(
                        teamCard -> {
                            if (oldStatus != newStatus) {
                                if (newStatus == MeetingStatus.COMPLETED) {
                                    teamCard.increaseMeetingCompletedCount();
                                }
                            } else {
                                if (newStatus == MeetingStatus.SCHEDULED) {
                                    sendMeetingNotHappenedEvent(teamCard, meetingLink);
                                }
                            }
                            if (teamCardStatus != null) {
                                teamCard.setStatus(teamCardStatus);
                            }
                            if (teamGrade != null) {
                                calculateAverageGrade(teamCard);
                            }
                            teamCardsRepository.saveAndFlush(teamCard);
                            log.info(
                                    "Updated team card {} info. "
                                            + "Current count: {}, status: {}, grade: {}",
                                    teamCardId,
                                    teamCard.getMeetingsCount(),
                                    teamCard.getStatus(),
                                    teamCard.getAverageGrade());
                        },
                        () -> log.warn("Team card {} not found", teamCardId));

    }

    @Override
    @Transactional
    public void handleMeetingDeleted(UUID teamCardId, UUID meetingId) {
        var teamCard = teamCardsRepository.findById(teamCardId)
                .orElseThrow(() -> new TeamCardNotFoundException(teamCardId));

        // Ищем MeetingGrade через репозиторий
        var meetingGradeOpt = meetingGradeRepository.findByMeetingIdAndTeamCardId(meetingId, teamCardId);
        
        if (meetingGradeOpt.isPresent()) {
            var meetingGrade = meetingGradeOpt.get();
            
            // Удаляем из коллекции TeamCard
            teamCard.getMeetingGrades().remove(meetingGrade);
            
            meetingGradeRepository.delete(meetingGrade);
            
            log.debug("Deleted meeting grade for meeting {} from team card {}",
                    meetingId, teamCardId);
            
            // Обновляем счетчики
            teamCard.setMeetingsCount(Math.max(0, teamCard.getMeetingsCount() - 1));
            
            // Пересчитываем среднюю оценку
            calculateAverageGrade(teamCard);
            
            // Сохраняем изменения в TeamCard
            teamCardsRepository.saveAndFlush(teamCard);
            
            log.info("Team card {} updated after meeting {} deletion. "
                    + "Remaining meetings: {}, New average grade: {}",
                    teamCardId, meetingId, teamCard.getMeetingsCount(),
                    teamCard.getAverageGrade());
        } else {
            log.warn("Meeting grade for meeting {} not found in team card {}",
                    meetingId, teamCardId);
        }
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
            var average = total.divide(
                    BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);
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
