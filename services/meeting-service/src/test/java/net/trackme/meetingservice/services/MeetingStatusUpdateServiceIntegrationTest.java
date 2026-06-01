package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

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
        scheduledMeeting.setRecordLink("TestLink");
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
        scheduledMeeting.setRecordLink("TestLink");
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

        Assertions.assertEquals(MeetingStatus.SCHEDULED, updatedMeeting.getStatus());
    }

    @Test
    void updateMeetingStatuses_notHappenedSuccess() {
        // Arrange
        var pastDate = OffsetDateTime.now().minusDays(4);

        var notHappenedMeeting1 = new Meeting();
        notHappenedMeeting1.setStartDate(pastDate);
        notHappenedMeeting1.setTeamStatus(TeamStatus.MANY_ISSUES);
        notHappenedMeeting1.setTeamCardId(UUID.randomUUID());
        notHappenedMeeting1.setStatus(MeetingStatus.SCHEDULED);

        var notHappenedMeeting2 = new Meeting();
        notHappenedMeeting2.setStartDate(pastDate);
        notHappenedMeeting2.setTeamCardId(UUID.randomUUID());
        notHappenedMeeting2.setStatus(MeetingStatus.SCHEDULED);

        meetingRepository.save(notHappenedMeeting1);
        meetingRepository.save(notHappenedMeeting2);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedMeeting1 = meetingRepository.findById(notHappenedMeeting1.getId()).orElseThrow();
        var updatedMeeting2 = meetingRepository.findById(notHappenedMeeting2.getId()).orElseThrow();

        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedMeeting1.getStatus());
        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedMeeting2.getStatus());
    }

    @Test
    void updateMeetingStatuses_withTeamStatusValue() {
        // Arrange - встреча с заполненным teamStatusValue
        var pastDate = OffsetDateTime.now().minusHours(2);

        var meeting = new Meeting();
        meeting.setRecordLink("TestLink");
        meeting.setNumber("TestNumber");
        meeting.setStartDate(pastDate);
        meeting.setTeamStatus(TeamStatus.MANY_ISSUES);
        meeting.setTeamStatusValue(java.math.BigDecimal.valueOf(0.25));
        meeting.setTeamCardId(UUID.randomUUID());
        meeting.setTasksCurrentMeeting("TestTasksCurrentMeeting");
        meeting.setTasksNextMeeting("TestTasksNextMeeting");
        meeting.setStatus(MeetingStatus.SCHEDULED);

        meetingRepository.save(meeting);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        Assertions.assertEquals(MeetingStatus.COMPLETED, updatedMeeting.getStatus());
        Assertions.assertNotNull(updatedMeeting.getTeamStatusValue());
    }

    @Test
    void updateMeetingStatuses_notHappenedWithTeamStatusValue() {
        var pastDate = OffsetDateTime.now().minusDays(4);

        var meeting = new Meeting();
        meeting.setStartDate(pastDate);
        meeting.setTeamStatus(TeamStatus.OK);
        meeting.setTeamStatusValue(java.math.BigDecimal.valueOf(1.0));
        meeting.setTeamCardId(UUID.randomUUID());
        meeting.setStatus(MeetingStatus.SCHEDULED);

        meetingRepository.save(meeting);

        meetingStatusUpdateService.updateMeetingStatuses();

        // Очищаем persistence context чтобы получить свежие данные из БД
        meetingRepository.flush();
        
        var updatedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedMeeting.getStatus());
        Assertions.assertNull(updatedMeeting.getTeamStatus());
        Assertions.assertNotNull(updatedMeeting.getTeamStatusValue());
    }

    @Test
    void updateMeetingStatuses_notHappenedHasUnfilledFields() {
        // Arrange - несостоявшаяся встреча без заполненных полей
        var pastDate = OffsetDateTime.now().minusDays(4);

        var meeting = new Meeting();
        meeting.setStartDate(pastDate);
        meeting.setTeamCardId(UUID.randomUUID());
        meeting.setStatus(MeetingStatus.SCHEDULED);

        meetingRepository.save(meeting);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedMeeting.getStatus());
        Assertions.assertNull(updatedMeeting.getTeamStatus());
    }

    @Test
    void updateMeetingStatuses_multipleExpiredMeetings() {
        // Arrange - создаём несколько встреч для проверки обработки нескольких за раз
        var pastDate = OffsetDateTime.now().minusHours(2);
        
        for (int i = 0; i < 5; i++) {
            var meeting = new Meeting();
            meeting.setRecordLink("TestLink" + i);
            meeting.setNumber("TestNumber" + i);
            meeting.setStartDate(pastDate);
            meeting.setTeamStatus(TeamStatus.MANY_ISSUES);
            meeting.setTeamCardId(UUID.randomUUID());
            meeting.setTasksCurrentMeeting("TestTasksCurrentMeeting" + i);
            meeting.setTasksNextMeeting("TestTasksNextMeeting" + i);
            meeting.setStatus(MeetingStatus.SCHEDULED);
            meetingRepository.save(meeting);
        }

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var meetings = meetingRepository.findAll();
        long completedCount = meetings.stream()
                .filter(m -> m.getStatus() == MeetingStatus.COMPLETED)
                .count();
        
        Assertions.assertEquals(5, completedCount, 
            "All 5 meetings should be completed");
    }
}