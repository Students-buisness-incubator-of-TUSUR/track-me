package net.trackme.meetingservice.services;

import net.trackme.meetingservice.configuration.AppProperties;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.messaging.own.MeetingReminderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeetingReminderServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingEventsProducer meetingEventsProducer;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private MeetingReminderService meetingReminderService;

    @Captor
    private ArgumentCaptor<MeetingReminderEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        when(appProperties.getAppUrl()).thenReturn("http://localhost:8082");
    }

    @Test
    void sendReminders_noMeetings_noEventsProduced() {
        when(meetingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());

        meetingReminderService.sendReminders();

        verify(meetingEventsProducer, never()).sendMeetingReminderEvent(any());
    }

    @Test
    void sendReminders_withMeetingThreeDaysAhead_sendsReminder() {
        UUID meetingId = UUID.randomUUID();
        UUID teamCardId = UUID.randomUUID();
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .teamName("Test Team")
                .trackerUsername("tracker")
                .trackerFullName("Трекер Трекерович")
                .startDate(OffsetDateTime.now().plusDays(3))
                .teamCardId(teamCardId)
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(meetingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(meeting))
                .thenReturn(List.of());

        meetingReminderService.sendReminders();

        verify(meetingEventsProducer, times(1)).sendMeetingReminderEvent(eventCaptor.capture());
        MeetingReminderEvent event = eventCaptor.getValue();
        assertEquals(meetingId, event.meetingId());
        assertEquals("Test Team", event.teamName());
        assertEquals("tracker", event.trackerUsername());
        assertEquals("Трекер Трекерович", event.trackerFullName());
        assertNotNull(event.meetingLink());
    }

    @Test
    void sendReminders_withMeetingOneDayAhead_sendsReminder() {
        UUID meetingId = UUID.randomUUID();
        UUID teamCardId = UUID.randomUUID();
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .teamName("Team One")
                .trackerUsername("tracker2")
                .startDate(OffsetDateTime.now().plusDays(1))
                .teamCardId(teamCardId)
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(meetingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(meeting));

        meetingReminderService.sendReminders();

        verify(meetingEventsProducer, times(1)).sendMeetingReminderEvent(eventCaptor.capture());
        MeetingReminderEvent event = eventCaptor.getValue();
        assertEquals(meetingId, event.meetingId());
        assertEquals(1, event.daysUntilMeeting());
    }

    @Test
    void sendReminders_bothDaysHaveMeetings_sendsTwoReminders() {
        UUID meetingId1 = UUID.randomUUID();
        UUID meetingId2 = UUID.randomUUID();

        Meeting meeting3Days = Meeting.builder()
                .id(meetingId1)
                .teamName("Team A")
                .trackerUsername("tracker1")
                .startDate(OffsetDateTime.now().plusDays(3))
                .teamCardId(UUID.randomUUID())
                .status(MeetingStatus.SCHEDULED)
                .build();

        Meeting meeting1Day = Meeting.builder()
                .id(meetingId2)
                .teamName("Team B")
                .trackerUsername("tracker2")
                .startDate(OffsetDateTime.now().plusDays(1))
                .teamCardId(UUID.randomUUID())
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(meetingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(meeting3Days))
                .thenReturn(List.of(meeting1Day));

        meetingReminderService.sendReminders();

        verify(meetingEventsProducer, times(2)).sendMeetingReminderEvent(any());
    }

    @Test
    void sendReminders_meetingWithNullTrackerUsername_skipped() {
        Meeting meeting = Meeting.builder()
                .id(UUID.randomUUID())
                .teamName("Team")
                .trackerUsername(null)
                .startDate(OffsetDateTime.now().plusDays(3))
                .teamCardId(UUID.randomUUID())
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(meetingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(meeting))
                .thenReturn(List.of());

        meetingReminderService.sendReminders();

        verify(meetingEventsProducer, never()).sendMeetingReminderEvent(any());
    }

    @Test
    void sendReminders_meetingLinkContainsMeetingId() {
        UUID meetingId = UUID.randomUUID();
        UUID teamCardId = UUID.randomUUID();
        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .teamName("Team")
                .trackerUsername("tracker")
                .startDate(OffsetDateTime.now().plusDays(3))
                .teamCardId(teamCardId)
                .status(MeetingStatus.SCHEDULED)
                .build();

        when(meetingRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(meeting))
                .thenReturn(List.of());

        meetingReminderService.sendReminders();

        verify(meetingEventsProducer).sendMeetingReminderEvent(eventCaptor.capture());
        String link = eventCaptor.getValue().meetingLink();
        assertNotNull(link);
        assertTrue(link.contains(meetingId.toString()));
        assertTrue(link.contains(teamCardId.toString()));
    }
}
