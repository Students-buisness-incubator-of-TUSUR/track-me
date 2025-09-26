package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.events.MeetingUpdatedEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingStatusUpdateService {
    private final MeetingRepository meetingRepository;
    private final MeetingEventsProducer meetingEventsProducer;
    private static final int BATCH_SIZE = 200;

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void updateMeetingStatuses() {
        log.info("Starting scheduled meeting status update");
        var now = OffsetDateTime.now();
        updateExpiredMeetings(now);
        updateNotHappenedMeetings(now);
        log.info("Scheduled meeting status update completed");
    }

    private void updateExpiredMeetings(OffsetDateTime now) {
        int page = 0;
        List<Meeting> expiredMeetings;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            expiredMeetings = meetingRepository
                    .findByStatusAndStartDateBefore(MeetingStatus.SCHEDULED, now, pageable);
            if (expiredMeetings.isEmpty()) break;
            for (Meeting meeting : expiredMeetings) {
                if (hasUnfilledFields(meeting)) {
                    meeting.setStatus(MeetingStatus.NOT_HAPPENED);
                    log.debug(
                            "Meeting {} set to NOT_HAPPENED due to unfilled fields",
                            meeting.getId());
                } else {
                    meeting.setStatus(MeetingStatus.COMPLETED);
                    log.debug("Meeting {} set to HAPPENED", meeting.getId());
                }
                sendMeetingEvent(meeting, MeetingStatus.SCHEDULED);
            }
            meetingRepository.saveAll(expiredMeetings);
            log.info("Updated {} expired meetings in batch {}", expiredMeetings.size(), page);
            page++;
        } while (expiredMeetings.size() == BATCH_SIZE);
    }

    private void updateNotHappenedMeetings(OffsetDateTime now) {
        OffsetDateTime threeDaysAgo = now.minusDays(3);
        int page = 0;
        List<Meeting> notHappenedMeetings;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            notHappenedMeetings = meetingRepository
                    .findByStatusAndStartDateBefore(
                            MeetingStatus.NOT_HAPPENED, threeDaysAgo, pageable);
            if (notHappenedMeetings.isEmpty()) break;
            for (Meeting meeting : notHappenedMeetings) {
                if (hasUnfilledFields(meeting)) {
                    meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
                    meeting.setTeamStatus(TeamStatus.MANY_ISSUES);
                    log.debug(
                            "Meeting {} set to COMPLETED_AS_NOT_HAPPENED after 3 days. Team status set to MANY_ISSUES",
                            meeting.getId());
                }
                sendMeetingEvent(meeting, MeetingStatus.NOT_HAPPENED);
            }
            meetingRepository.saveAll(notHappenedMeetings);
            log.info(
                    "Updated {} not happened meetings to completed in batch {}",
                    notHappenedMeetings.size(), page);
            page++;
        } while (notHappenedMeetings.size() == BATCH_SIZE);
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

    private void sendMeetingEvent(Meeting meeting, MeetingStatus oldStatus) {
        var event = MeetingUpdatedEvent.builder()
                .meetingId(meeting.getId())
                .newStatus(meeting.getStatus())
                .oldStatus(oldStatus)
                .teamStatus(meeting.getTeamStatus())
                .teamCardId(meeting.getTeamCardId())
                .teamGrade(meeting.getTeamStatus().getValue())
                .build();
        meetingEventsProducer.sendMeetingUpdatedEvent(event);
    }
}
