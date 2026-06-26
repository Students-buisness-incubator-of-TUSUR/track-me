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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCardEventsListenerTest extends AbstractIntegrationTest {

    private static final String INVITE_TEMPLATE = "email-meeting-invite.html";
    private static final String REMINDER_TEMPLATE = "email-meeting-reminder.html";
    private static final String INVITE_SUBJECT = "Приглашение на встречу";

    @Mock
    private ConsumerRecord<String, MeetingNotHappenedEvent> meetingNotHappenedRecord;

    @Mock
    private ConsumerRecord<String, List<LinkedHashMap<String, String>>> teamCardSummaryRecord;

    @Mock
    private ConsumerRecord<String, List<LinkedHashMap<String, String>>> teamCardLowGradeSummaryRecord;

    @Mock
    private ConsumerRecord<String, MeetingInviteEvent> meetingInviteRecord;

    @Mock
    private ConsumerRecord<String, MeetingReminderEvent> meetingReminderRecord;

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

    @Test
    void onTeamCardLowGradeSummaryEvent() {
        List<LinkedHashMap<String, String>> event = new ArrayList<>();
        when(teamCardLowGradeSummaryRecord.value()).thenReturn(event);

        teamCardEventsListener.onTeamCardLowGradeSummaryEvent(teamCardLowGradeSummaryRecord);

        verify(notificationService).sendTeamCardLowGradeSummary(event);
    }

    @Test
    void onMeetingInviteEvent_withTrackerEmail_sendsInviteToTracker() {
        UUID meetingId = UUID.randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now();
        MeetingInviteEvent event = new MeetingInviteEvent(
                meetingId, "Test Team", "tracker", "Трекер Трекерович",
                "tracker@example.com", "tracker", startDate, "http://meeting.link");
        when(meetingInviteRecord.value()).thenReturn(event);

        teamCardEventsListener.onMeetingInviteEvent(meetingInviteRecord);

        verify(notificationService).sendMeetingEmail(
                "tracker@example.com", "Трекер Трекерович", "Test Team",
                "http://meeting.link", startDate, INVITE_TEMPLATE, INVITE_SUBJECT, Map.of());
        verify(notificationService, never()).sendMeetingEmailByUsername(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onMeetingInviteEvent_withDifferentCreator_sendsInviteToBoth() {
        UUID meetingId = UUID.randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now();
        MeetingInviteEvent event = new MeetingInviteEvent(
                meetingId, "Test Team", "tracker", "Трекер Трекерович",
                "tracker@example.com", "creator_user", startDate, "http://meeting.link");
        when(meetingInviteRecord.value()).thenReturn(event);

        teamCardEventsListener.onMeetingInviteEvent(meetingInviteRecord);

        verify(notificationService).sendMeetingEmail(
                "tracker@example.com", "Трекер Трекерович", "Test Team",
                "http://meeting.link", startDate, INVITE_TEMPLATE, INVITE_SUBJECT, Map.of());
        verify(notificationService).sendMeetingEmailByUsername(
                "creator_user", "Test Team", "http://meeting.link", startDate,
                INVITE_TEMPLATE, INVITE_SUBJECT, Map.of());
    }

    @Test
    void onMeetingInviteEvent_withoutTrackerEmail_skipsTrackerInvite() {
        UUID meetingId = UUID.randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now();
        MeetingInviteEvent event = new MeetingInviteEvent(
                meetingId, "Test Team", "tracker", "Трекер Трекерович",
                null, "creator_user", startDate, "http://meeting.link");
        when(meetingInviteRecord.value()).thenReturn(event);

        teamCardEventsListener.onMeetingInviteEvent(meetingInviteRecord);

        verify(notificationService, never()).sendMeetingEmail(any(), any(), any(), any(), any(), any(), any(), any());
        verify(notificationService).sendMeetingEmailByUsername(
                "creator_user", "Test Team", "http://meeting.link", startDate,
                INVITE_TEMPLATE, INVITE_SUBJECT, Map.of());
    }

    @Test
    void onMeetingReminderEvent_withTrackerUsername_sendsReminder() {
        UUID meetingId = UUID.randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now().plusDays(3);
        MeetingReminderEvent event = new MeetingReminderEvent(
                meetingId, "Test Team", "tracker", "Трекер Трекерович",
                startDate, "http://meeting.link", 3);
        when(meetingReminderRecord.value()).thenReturn(event);

        teamCardEventsListener.onMeetingReminderEvent(meetingReminderRecord);

        verify(notificationService).sendMeetingEmailByUsername(
                "tracker", "Test Team", "http://meeting.link", startDate,
                REMINDER_TEMPLATE, "Напоминание: встреча через 3 дня", Map.of("daysUntilMeeting", 3));
    }

    @Test
    void onMeetingReminderEvent_withoutTrackerUsername_skipsReminder() {
        UUID meetingId = UUID.randomUUID();
        MeetingReminderEvent event = new MeetingReminderEvent(
                meetingId, "Test Team", null, null,
                OffsetDateTime.now(), "http://meeting.link", 1);
        when(meetingReminderRecord.value()).thenReturn(event);

        teamCardEventsListener.onMeetingReminderEvent(meetingReminderRecord);

        verify(notificationService, never()).sendMeetingEmailByUsername(any(), any(), any(), any(), any(), any(), any());
    }
}
