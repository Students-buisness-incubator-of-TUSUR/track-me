package net.trackme.sso.messaging;

import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.services.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamCardEventsListenerTest extends AbstractIntegrationTest {

    @Mock
    private ConsumerRecord<String, MeetingNotHappenedEvent> meetingNotHappenedRecord;

    @Mock
    private ConsumerRecord<String, List<LinkedHashMap<String, String>>> teamCardSummaryRecord;

    @Autowired
    private TeamCardEventsListener teamCardEventsListener;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void onMeetingNotHappenedEvent() {
        // Arrange
        var teamCardUsername = "test username";
        var teamCardName = "test team card";
        var streamName = "test stream";
        var meetingLink = "test link";
        var trackerFullName = "Петров Петр Петрович";
        MeetingNotHappenedEvent event = new MeetingNotHappenedEvent(
                teamCardUsername,
                teamCardName,
                streamName,
                meetingLink,
                trackerFullName
        );
        when(meetingNotHappenedRecord.value()).thenReturn(event);

        // Act
        teamCardEventsListener.onMeetingNotHappenedEvent(meetingNotHappenedRecord);

        // Assert
        verify(notificationService).sendMeetingNotHappenedNotification(
                teamCardUsername,
                teamCardName,
                streamName,
                meetingLink,
                trackerFullName);
    }

    @Test
    void onTeamCardSummaryEvent() {
        // Arrange
        List<LinkedHashMap<String, String>> event = new ArrayList<>();
        when(teamCardSummaryRecord.value()).thenReturn(event);

        // Act
        teamCardEventsListener.onTeamCardSummaryEvent(teamCardSummaryRecord);

        // Assert
        verify(notificationService).sendTeamCardSummary(event);
    }
}