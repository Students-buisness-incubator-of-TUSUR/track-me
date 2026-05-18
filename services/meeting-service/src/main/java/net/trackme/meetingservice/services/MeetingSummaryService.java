package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.configuration.AppProperties;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingSpecification;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.messaging.own.MeetingSummaryEvent;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummaryService {
    /**
     * Период выборки несостоявщихся встреч.
     */
    private static final int MEETING_SUMMARY_PERIOD = 7;

    /**
     * Репозиторий встреч.
     */
    private final MeetingRepository meetingRepository;

    /**
     * Отправитель сообщений.
     */
    private final MeetingEventsProducer meetingEventsProducer;

    /**
     * Настройки приложения.
     */
    private final AppProperties appProperties;

    /**
     * Сообщить о пропущенных встречах за период.
     */
    @Transactional
    @Scheduled(cron = "0 0 9 * * 1", zone = "Asia/Tomsk")
    public void reportAboutNotHappenedMeetings() {
        log.info("Scheduled not happened meetings summary started");

        var now = OffsetDateTime.now();
        var dateAfter = now.minusDays(MEETING_SUMMARY_PERIOD);

        sendNotHappenedMeetingSummary(dateAfter, now);
        log.info("Scheduled not happened meetings summary completed");
    }

    private void sendNotHappenedMeetingSummary(OffsetDateTime dateAfter, OffsetDateTime now) {
        Specification<Meeting> isAfterDate = MeetingSpecification.startDateAfter(dateAfter);
        Specification<Meeting> isCancelled = MeetingSpecification.withStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        Specification<Meeting> isScheduled = MeetingSpecification.withStatus(MeetingStatus.SCHEDULED);
        Specification<Meeting> isOverdue = MeetingSpecification.startDateBefore(now);

        Specification<Meeting> statusCondition = isCancelled.or(isScheduled.and(isOverdue));
        Specification<Meeting> spec = isAfterDate.and(statusCondition);

        List<Meeting> meetings = meetingRepository.findAll(spec);
        if (meetings.isEmpty()) {
            log.debug("No missed meetings to report for the last 7 days");
            return;
        }

        List<MeetingSummaryEvent> meetingSummaryEvents = new ArrayList<>();
        for (Meeting meeting : meetings) {
            String trackerFullName = meeting.getTrackerFullName() != null
                    ? meeting.getTrackerFullName()
                    : "Не назначен";

            var meetingSummaryEvent = MeetingSummaryEvent.builder()
                    .meetingNumber(meeting.getNumber())
                    .meetingLink(getMeetingLink(meeting))
                    .teamCardId(meeting.getTeamCardId())
                    .trackerFullName(trackerFullName)
                    .build();

            meetingSummaryEvents.add(meetingSummaryEvent);
        }

        meetingEventsProducer.sendMeetingSummaryEvents(meetingSummaryEvents);
    }

    private String getMeetingLink(Meeting meeting) {
        var httpUrl = appProperties.getAppUrl() + "/meeting/{meetingId}";
        return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
                .queryParam("teamId", "{teamId}")
                .build(meeting.getId(), meeting.getTeamCardId())
                .toString();
    }
}
