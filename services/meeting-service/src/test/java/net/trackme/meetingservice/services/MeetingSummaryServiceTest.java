package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
        var meeting = new Meeting();
        meeting.setTeamCardId(UUID.randomUUID());
        meeting.setNumber("test number");
        meeting.setLink("test link");
        meeting.setStartDate(OffsetDateTime.now().minusDays(3));
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(meeting);

        // Act
        meetingSummaryService.reportAboutNotHappenedMeetings();

        // Assert
        verify(kafkaTemplate).send(any(Message.class));
    }
}