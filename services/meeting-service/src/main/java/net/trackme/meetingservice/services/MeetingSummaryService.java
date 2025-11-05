package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.events.MeetingSummaryEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummaryService {
    private final MeetingRepository meetingRepository;
    private final MeetingEventsProducer meetingEventsProducer;

    @Transactional
    @Scheduled(cron = "* 0 12 * * 7")
    public void reportAboutNotHappenedMeetings() {
        log.info("Scheduled not happened meetings summary started");
        var now = OffsetDateTime.now();
        sendNotHappenedMeetingSummary(now);
        log.info("Scheduled not happened meetings summary completed");
    }

    private void sendNotHappenedMeetingSummary(OffsetDateTime now) {
        var dateAfter = now.minusDays(7);
        List<MeetingSummaryEvent> meetingSummaryEvents = new ArrayList<>();

        List<Meeting> meetings = meetingRepository
                .findByStatusAndStartDateAfter(MeetingStatus.NOT_HAPPENED, dateAfter);
        meetings.addAll(meetingRepository
                .findByStatusAndStartDateAfter(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, dateAfter));

        if (meetings.isEmpty())
            return;

        for (Meeting meeting : meetings) {
            var meetingSummaryEvent = MeetingSummaryEvent.builder()
                    .meetingNumber(meeting.getNumber())
                    .meetingLink(meeting.getLink())
                    .teamCardId(meeting.getTeamCardId())
                    .build();
            meetingSummaryEvents.add(meetingSummaryEvent);
        }

        meetingEventsProducer.sendMeetingSummaryEvents(meetingSummaryEvents);
    }
}
