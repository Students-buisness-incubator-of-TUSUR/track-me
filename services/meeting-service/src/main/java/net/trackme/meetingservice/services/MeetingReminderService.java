package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.configuration.AppProperties;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.messaging.own.MeetingReminderEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingReminderService {

    private final MeetingRepository meetingRepository;
    private final MeetingEventsProducer meetingEventsProducer;
    private final AppProperties appProperties;

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Tomsk")
    public void sendReminders() {
        log.info("Starting scheduled meeting reminder check");
        sendRemindersForDaysAhead(3);
        sendRemindersForDaysAhead(1);
        log.info("Meeting reminder check completed");
    }

    private void sendRemindersForDaysAhead(int daysAhead) {
        ZoneId zone = ZoneId.of("Asia/Tomsk");
        LocalDate targetDate = LocalDate.now(zone).plusDays(daysAhead);
        OffsetDateTime from = targetDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = targetDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<Meeting> meetings = meetingRepository
                .findByStatusAndStartDateGreaterThanEqualAndStartDateLessThan(
                        MeetingStatus.SCHEDULED, from, to);

        log.info("Found {} meetings for reminder in {} days", meetings.size(), daysAhead);

        for (Meeting meeting : meetings) {
            if (meeting.getTrackerUsername() == null) {
                log.warn("Meeting {} has no trackerUsername, skipping reminder", meeting.getId());
                continue;
            }
            var event = MeetingReminderEvent.builder()
                    .meetingId(meeting.getId())
                    .teamName(meeting.getTeamName())
                    .trackerUsername(meeting.getTrackerUsername())
                    .trackerFullName(meeting.getTrackerFullName())
                    .startDate(meeting.getStartDate())
                    .meetingLink(getMeetingLink(meeting))
                    .daysUntilMeeting(daysAhead)
                    .build();
            meetingEventsProducer.sendMeetingReminderEvent(event);
            log.debug("Sent {}-day reminder for meeting {}", daysAhead, meeting.getId());
        }
    }

    private String getMeetingLink(Meeting meeting) {
        var httpUrl = appProperties.getAppUrl() + "/meeting/{meetingId}";
        return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
                .queryParam("teamId", "{teamId}")
                .build(meeting.getId(), meeting.getTeamCardId())
                .toString();
    }
}
