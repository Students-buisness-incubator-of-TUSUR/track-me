package net.trackme.backend.messaging;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.services.teamcard.TeamCardSummaryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingEventsListenerTest extends BaseApplicationTest {

    @Mock
    private ConsumerRecord<String, List<LinkedHashMap<String, String>>> record;

    @Autowired
    private MeetingEventsListener meetingEventsListener;

    @MockitoBean
    private TeamCardSummaryService teamCardSummaryService;

    @Test
    void onMeetingSummaryEvent() {
        // Arrange
        List<LinkedHashMap<String, String>> event = new ArrayList<>();
        when(record.value()).thenReturn(event);

        // Act
        meetingEventsListener.onMeetingSummaryEvent(record);

        // Assert
        verify(teamCardSummaryService).sendTeamCardsSummary(event);
    }
}