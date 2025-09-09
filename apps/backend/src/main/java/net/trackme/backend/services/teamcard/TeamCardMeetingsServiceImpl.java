package net.trackme.backend.services.teamcard;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.MeetingGradeRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCardMeetingsServiceImpl implements TeamCardMeetingsService {

    private final TeamCardsRepository teamCardsRepository;

    private final MeetingGradeRepository meetingGradeRepository;

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
                                    "Increased meeting count for team card {}. Current count: {}. Average grade: {}",
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
                                   BigDecimal teamGrade) {
        meetingGradeRepository.findByMeetingIdAndTeamCardId(meetingId, teamCardId)
                .ifPresent(meetingGrade -> meetingGrade.setGrade(teamGrade));
        teamCardsRepository.findById(teamCardId)
                .ifPresentOrElse(
                        teamCard -> {
                            if (oldStatus != newStatus) {
                                if (newStatus == MeetingStatus.COMPLETED) {
                                    teamCard.increaseMeetingCompletedCount();
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
                                    "Updated team card {} info. Current count: {}, status: {}, grade: {}",
                                    teamCardId,
                                    teamCard.getMeetingsCount(),
                                    teamCard.getStatus(),
                                    teamCard.getAverageGrade());
                        },
                        () -> log.warn("Team card {} not found", teamCardId));

    }

    private void calculateAverageGrade(TeamCard teamCard) {
        var meetingGrades = teamCard.getMeetingGrades();

        if (meetingGrades.isEmpty()) {
            teamCard.setAverageGrade(BigDecimal.ZERO);
        } else {
            var total = meetingGrades.stream()
                    .map(grade -> grade.getGrade() != null ? grade.getGrade() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var average = total.divide(
                    BigDecimal.valueOf(meetingGrades.size()), 2, RoundingMode.HALF_UP);
            teamCard.setAverageGrade(average);
        }
    }
}
