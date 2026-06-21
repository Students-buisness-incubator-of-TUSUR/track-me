package net.trackme.backend.messaging;

import net.trackme.backend.services.teamcard.TeamCardMeetingsService;
import net.trackme.backend.services.teamcard.TeamCardSummaryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingEventsListenerTest {

    @Mock
    private TeamCardSummaryService teamCardSummaryService;

    @Mock
    private TeamCardMeetingsService teamCardMeetingsService;

    @InjectMocks
    private MeetingEventsListener meetingEventsListener;

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
