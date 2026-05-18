package net.trackme.backend.messaging;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.services.teamcard.TeamCardMeetingsService;
import net.trackme.backend.services.teamcard.TeamCardSummaryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

class MeetingEventsListenerTest extends BaseApplicationTest {

    @Autowired
    private MeetingEventsListener meetingEventsListener;

    @MockitoBean
    private TeamCardSummaryService teamCardSummaryService;

    @MockitoBean
    private TeamCardMeetingsService teamCardMeetingsService;

    @Test
    void onMeetingSummaryEvent() {
        List<LinkedHashMap<String, String>> event = new ArrayList<>();
        ConsumerRecord<String, List<LinkedHashMap<String, String>>> summaryRecord =
                new ConsumerRecord<>("meeting-summary", 0, 0L, "key", event);

        meetingEventsListener.onMeetingSummaryEvent(summaryRecord);

        verify(teamCardSummaryService).sendTeamCardsSummary(event);
    }

    @Test
    void onMeetingDeletedEvent() {
        UUID meetingId = UUID.randomUUID();
        UUID teamCardId = UUID.randomUUID();
        MeetingDeletedEvent event = new MeetingDeletedEvent(
                meetingId, teamCardId, OffsetDateTime.now());

        ConsumerRecord<String, MeetingDeletedEvent> deletedRecord =
                new ConsumerRecord<>("meeting-deleted", 0, 0L, "key", event);

        meetingEventsListener.onMeetingDeletedEvent(deletedRecord);

        verify(teamCardMeetingsService).handleMeetingDeleted(teamCardId, meetingId);
    }
}