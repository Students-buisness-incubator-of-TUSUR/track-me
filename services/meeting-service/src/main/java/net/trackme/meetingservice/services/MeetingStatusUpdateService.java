package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.configuration.AppProperties;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.messaging.own.MeetingUpdatedEvent;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingStatusUpdateService {
    /**
     * Размер пакета.
     */
    private static final int BATCH_SIZE = 200;

    /**
     * Период до завершения несостоявщейся встречи.
     */
    private static final int NOT_HAPPENED_MEETING_PERIOD = 3;

    /**
     * Репозиторий встреч.
     */
    private final MeetingRepository meetingRepository;

    /**
     * Поставщик сообщений о встречах.
     */
    private final MeetingEventsProducer meetingEventsProducer;

    /**
     * Настройки приложения.
     */
    private final AppProperties appProperties;

    /**
     * Автоматическое обновление статусов встреч.
     */
    @Transactional
    @Scheduled(cron = "0 0 5 * * *")
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
            if (expiredMeetings.isEmpty()) {
                break;
            }
            for (Meeting meeting : expiredMeetings) {
                if (hasUnfilledFields(meeting)) {
                    log.debug(
                            "Meeting {} remained to SCHEDULED due to unfilled fields",
                            meeting.getId());
                } else {
                    meeting.setStatus(MeetingStatus.COMPLETED);
                    log.debug("Meeting {} set to COMPLETED", meeting.getId());
                }
                sendMeetingEvent(meeting, MeetingStatus.SCHEDULED);
            }
            meetingRepository.saveAll(expiredMeetings);
            log.info("Updated {} expired meetings in batch {}", expiredMeetings.size(), page);
            page++;
        } while (expiredMeetings.size() == BATCH_SIZE);
    }

    private void updateNotHappenedMeetings(OffsetDateTime now) {
        OffsetDateTime threeDaysAgo = now.minusDays(NOT_HAPPENED_MEETING_PERIOD);
        int page = 0;
        List<Meeting> notHappenedMeetings;
        do {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            notHappenedMeetings = meetingRepository
                    .findByStatusAndStartDateBefore(
                            MeetingStatus.SCHEDULED, threeDaysAgo, pageable);
            if (notHappenedMeetings.isEmpty()) {
                break;
            }
            for (Meeting meeting : notHappenedMeetings) {
                if (hasUnfilledFields(meeting)) {
                    meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
                    meeting.setTeamStatus(null);
                    log.debug(
                            "Meeting {} set to COMPLETED_AS_NOT_HAPPENED after {} days",
                            meeting.getId(),
                            NOT_HAPPENED_MEETING_PERIOD);
                }
                sendMeetingEvent(meeting, MeetingStatus.SCHEDULED);
            }
            meetingRepository.saveAll(notHappenedMeetings);
            log.info(
                    "Updated {} not happened meetings to completed in batch {}",
                    notHappenedMeetings.size(), page);
            page++;
        } while (notHappenedMeetings.size() == BATCH_SIZE);
    }

    private boolean hasUnfilledFields(Meeting meeting) {
        return meeting.getRecordLink() == null
                || meeting.getNumber() == null
                || meeting.getStartDate() == null
                || meeting.getTeamStatus() == null
                || meeting.getTeamCardId() == null
                || meeting.getTasksCurrentMeeting() == null
                || meeting.getTasksNextMeeting() == null;
    }

    private void sendMeetingEvent(Meeting meeting, MeetingStatus oldStatus) {
        var event = MeetingUpdatedEvent.builder()
                .meetingId(meeting.getId())
                .newStatus(meeting.getStatus())
                .oldStatus(oldStatus)
                .teamStatus(meeting.getTeamStatus())
                .teamCardId(meeting.getTeamCardId())
                .teamGrade(computeTeamGrade(meeting.getTeamStatus()))
                .meetingLink(getMeetingLink(meeting))
                .build();
        meetingEventsProducer.sendMeetingUpdatedEvent(event);
    }

    private double computeTeamGrade(TeamStatus teamStatus) {
        return teamStatus != null ? teamStatus.getValue() : 0.0;
    }

    private String getMeetingLink(Meeting meeting) {
        var httpUrl = appProperties.getAppUrl() + "/meeting/{meetingId}";
        return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
                .queryParam("teamId", "{teamId}")
                .build(meeting.getId(), meeting.getTeamCardId())
                .toString();
    }
}
