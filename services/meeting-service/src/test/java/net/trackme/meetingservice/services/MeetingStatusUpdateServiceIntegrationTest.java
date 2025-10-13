package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
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
    void updateMeetingStatuses_integrationTest() {
        // Arrange
        var pastDate = OffsetDateTime.now().minusHours(2);

        var expiredMeeting = new Meeting();
        expiredMeeting.setStartDate(pastDate);
        expiredMeeting.setTeamStatus(TeamStatus.MANY_ISSUES);
        expiredMeeting.setTeamCardId(UUID.randomUUID());
        expiredMeeting.setStatus(MeetingStatus.SCHEDULED);

        var expiredMeetingWithoutUnfilledFields = new Meeting();
        expiredMeetingWithoutUnfilledFields.setLink("TestLink");
        expiredMeetingWithoutUnfilledFields.setNumber("TestNumber");
        expiredMeetingWithoutUnfilledFields.setStartDate(pastDate);
        expiredMeetingWithoutUnfilledFields.setTeamStatus(TeamStatus.MANY_ISSUES);
        expiredMeetingWithoutUnfilledFields.setTeamCardId(UUID.randomUUID());
        expiredMeetingWithoutUnfilledFields.setTasksCurrentMeeting("TestTasksCurrentMeeting");
        expiredMeetingWithoutUnfilledFields.setTasksNextMeeting("TestTasksNextMeeting");
        expiredMeetingWithoutUnfilledFields.setStatus(MeetingStatus.SCHEDULED);

        var notHappenedMeeting = new Meeting();
        notHappenedMeeting.setStartDate(OffsetDateTime.now().minusDays(4));
        notHappenedMeeting.setTeamStatus(TeamStatus.MANY_ISSUES);
        notHappenedMeeting.setTeamCardId(UUID.randomUUID());
        notHappenedMeeting.setStatus(MeetingStatus.NOT_HAPPENED);

        meetingRepository.save(expiredMeeting);
        meetingRepository.save(expiredMeetingWithoutUnfilledFields);
        meetingRepository.save(notHappenedMeeting);

        // Act
        meetingStatusUpdateService.updateMeetingStatuses();

        // Assert
        var updatedExpiredMeeting = meetingRepository.findById(expiredMeeting.getId()).orElseThrow();
        var updatedExpiredMeetingWithoutUnfilledFields = meetingRepository.findById(expiredMeetingWithoutUnfilledFields.getId()).orElseThrow();
        var updatedNotHappenedMeeting = meetingRepository.findById(notHappenedMeeting.getId()).orElseThrow();

        Assertions.assertEquals(MeetingStatus.NOT_HAPPENED, updatedExpiredMeeting.getStatus());
        Assertions.assertEquals(MeetingStatus.COMPLETED, updatedExpiredMeetingWithoutUnfilledFields.getStatus());
        Assertions.assertEquals(MeetingStatus.COMPLETED_AS_NOT_HAPPENED, updatedNotHappenedMeeting.getStatus());
        Assertions.assertEquals(TeamStatus.MANY_ISSUES, updatedNotHappenedMeeting.getTeamStatus());
    }
}