package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.events.MeetingUpdatedEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
class MeetingStatusUpdateServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeetingStatusUpdateService meetingStatusUpdateService;

    @Autowired
    private MeetingRepository meetingRepository;

    @MockitoBean
    private MeetingEventsProducer meetingEventsProducer;

    @Test
    void updateMeetingStatuses_success() {
        // Arrange
        var pastDate = OffsetDateTime.now().minusHours(2);

        var scheduledMeeting = new Meeting();
        scheduledMeeting.setLink("TestLink");
        scheduledMeeting.setNumber("TestNumber");
        scheduledMeeting.setStartDate(pastDate);
        scheduledMeeting.setTeamStatus(TeamStatus.MANY_ISSUES);
        scheduledMeeting.setTeamCardId(UUID.randomUUID());
        scheduledMeeting.setTasksCurrentMeeting("TestTasksCurrentMeeting");
        scheduledMeeting.setTasksNextMeeting("TestTasksNextMeeting");
        scheduledMeeting.setStatus(MeetingStatus.SCHEDULED);

        meetingRepository.save(scheduledMeeting);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedMeeting = meetingRepository.findById(scheduledMeeting.getId()).orElseThrow();

        Assertions.assertEquals(MeetingStatus.COMPLETED, updatedMeeting.getStatus());
    }

    @Test
    void updateMeetingStatuses_hasUnfilledFields() {
        // Arrange
        var pastDate = OffsetDateTime.now().minusHours(2);

        var scheduledMeeting = new Meeting();
        scheduledMeeting.setLink("TestLink");
        scheduledMeeting.setNumber("TestNumber");
        scheduledMeeting.setStartDate(pastDate);
        scheduledMeeting.setTeamStatus(TeamStatus.MANY_ISSUES);
        scheduledMeeting.setTeamCardId(UUID.randomUUID());
        scheduledMeeting.setTasksCurrentMeeting("TestTasksCurrentMeeting");
        scheduledMeeting.setStatus(MeetingStatus.SCHEDULED);

        meetingRepository.save(scheduledMeeting);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedMeeting = meetingRepository.findById(scheduledMeeting.getId()).orElseThrow();

        Assertions.assertEquals(MeetingStatus.NOT_HAPPENED, updatedMeeting.getStatus());
    }

    @Test
    void updateMeetingStatuses_notHappenedSuccess() {
        // Arrange
        var pastDate = OffsetDateTime.now().minusDays(4);

        var notHappenedMeeting1 = new Meeting();
        notHappenedMeeting1.setStartDate(pastDate);
        notHappenedMeeting1.setTeamStatus(TeamStatus.MANY_ISSUES);
        notHappenedMeeting1.setTeamCardId(UUID.randomUUID());
        notHappenedMeeting1.setStatus(MeetingStatus.NOT_HAPPENED);

        var notHappenedMeeting2 = new Meeting();
        notHappenedMeeting2.setStartDate(pastDate);
        notHappenedMeeting2.setTeamCardId(UUID.randomUUID());
        notHappenedMeeting2.setStatus(MeetingStatus.NOT_HAPPENED);

        meetingRepository.save(notHappenedMeeting1);
        meetingRepository.save(notHappenedMeeting2);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedMeeting1 = meetingRepository.findById(notHappenedMeeting1.getId()).orElseThrow();
        var updatedMeeting2 = meetingRepository.findById(notHappenedMeeting2.getId()).orElseThrow();

        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedMeeting1.getStatus());
        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedMeeting2.getStatus());
        verify(meetingEventsProducer, times(2)).sendMeetingUpdatedEvent(any(MeetingUpdatedEvent.class));
    }

    @Test
    void updateMeetingStatuses_isEmpty() {
        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        verify(meetingEventsProducer, times(0)).sendMeetingUpdatedEvent(any(MeetingUpdatedEvent.class));
    }

}