package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.messaging.own.MeetingSummaryEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class MeetingSummaryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingSummaryService meetingSummaryService;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void reportAboutNotHappenedMeetings_success() {
        // Arrange
        UUID teamId = UUID.randomUUID();
        meetingRepository.deleteAll();
        
        Meeting cancelledMeeting = new Meeting();
        cancelledMeeting.setTeamCardId(teamId);
        cancelledMeeting.setNumber("CANCELLED-1");
        cancelledMeeting.setStartDate(OffsetDateTime.now().minusDays(3));
        cancelledMeeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        meetingRepository.save(cancelledMeeting);

        Meeting overdueMeeting = new Meeting();
        overdueMeeting.setTeamCardId(teamId);
        overdueMeeting.setNumber("OVERDUE-2");
        overdueMeeting.setStartDate(OffsetDateTime.now().minusDays(3));
        overdueMeeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(overdueMeeting);

        Meeting futureMeeting = new Meeting();
        futureMeeting.setTeamCardId(teamId);
        futureMeeting.setNumber("FUTURE-3");
        futureMeeting.setStartDate(OffsetDateTime.now().plusDays(14));
        futureMeeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(futureMeeting);

        Meeting tooOldMeeting = new Meeting();
        tooOldMeeting.setTeamCardId(teamId);
        tooOldMeeting.setNumber("OLD-4");
        tooOldMeeting.setStartDate(OffsetDateTime.now().minusDays(14));
        tooOldMeeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(tooOldMeeting);

        // Act
        meetingSummaryService.reportAboutNotHappenedMeetings();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass((Class) Message.class);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> capturedMessage = messageCaptor.getValue();

        @SuppressWarnings("unchecked")
        List<MeetingSummaryEvent> sentEvents = (List<MeetingSummaryEvent>) capturedMessage.getPayload();

        // Assert
        boolean hasCancelled = sentEvents
            .stream()
            .anyMatch(e -> "CANCELLED-1".equals(e.meetingNumber()));

        boolean hasOverdue = sentEvents
            .stream()
            .anyMatch(e -> "OVERDUE-2".equals(e.meetingNumber()));

        assertEquals(
            2,
            sentEvents.size(),
            "Должны быть найдены только отмененные и просроченные встречи в рамках 7 дней"
        );

        assertTrue(
            hasCancelled,
            "В отчете должна быть явно отмененная встреча"
        );

        assertTrue(
            hasOverdue,
            "В отчете должна быть забытая (просроченная) встреча"
        );
    }
}