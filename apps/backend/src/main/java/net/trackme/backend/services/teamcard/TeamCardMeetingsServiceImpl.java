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
                            teamCardsRepository.save(teamCard);
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
                .ifPresent(meetingGrade -> meetingGrade.setGrade(teamGrade));
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
                            teamCardsRepository.save(teamCard);
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
