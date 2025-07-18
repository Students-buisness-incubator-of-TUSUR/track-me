package net.trackme.meetingservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingStatusUpdateService {
    private final MeetingRepository meetingRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updateMeetingStatuses() {
        log.info("Starting scheduled meeting status update");

        var now = OffsetDateTime.now();

        updateExpiredMeetings(now);

        updateNotHappenedMeetings(now);

        log.info("Scheduled meeting status update completed");
    }

    private void updateExpiredMeetings(OffsetDateTime now) {
        List<Meeting> expiredMeetings = meetingRepository
                .findByStatusAndStartDateBefore(MeetingStatus.SCHEDULED, now);

        for (Meeting meeting : expiredMeetings) {
            if (hasUnfilledFields(meeting)) {
                meeting.setStatus(MeetingStatus.NOT_HAPPENED);
                log.info("Meeting {} set to NOT_HAPPENED due to unfilled fields", meeting.getId());
            } else {
                meeting.setStatus(MeetingStatus.COMPLETED);
                log.info("Meeting {} set to HAPPENED", meeting.getId());
            }
        }

        if (!expiredMeetings.isEmpty()) {
            meetingRepository.saveAll(expiredMeetings);
            log.info("Updated {} expired meetings", expiredMeetings.size());
        }
    }

    private void updateNotHappenedMeetings(OffsetDateTime now) {
        OffsetDateTime threeDaysAgo = now.minusDays(3);
        List<Meeting> notHappenedMeetings = meetingRepository
                .findByStatusAndStartDateBefore(MeetingStatus.NOT_HAPPENED, threeDaysAgo);

        for (Meeting meeting : notHappenedMeetings) {
            if (hasUnfilledFields(meeting)) {
                meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
                log.info(
                        "Meeting {} set to COMPLETED_AS_NOT_HAPPENED after 3 days",
                        meeting.getId());
            }
        }

        if (!notHappenedMeetings.isEmpty()) {
            meetingRepository.saveAll(notHappenedMeetings);
            log.info("Updated {} not happened meetings to completed", notHappenedMeetings.size());
        }
    }

    private boolean hasUnfilledFields(Meeting meeting) {
        return meeting.getLink() == null ||
               meeting.getNumber() == null ||
               meeting.getStartDate() == null ||
               meeting.getTeamStatus() == null ||
               meeting.getTeamCardId() == null ||
               meeting.getTasksCurrentMeeting() == null ||
               meeting.getTasksNextMeeting() == null;
    }
}
