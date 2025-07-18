package net.trackme.meetingservice.services;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

class MeetingStatusUpdateServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeetingStatusUpdateService meetingStatusUpdateService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Test
    void updateMeetingStatuses_integrationTest() {
        // given
        var pastDate = OffsetDateTime.now().minusHours(2);

        var expiredMeeting = new Meeting();
        expiredMeeting.setStatus(MeetingStatus.SCHEDULED);
        expiredMeeting.setStartDate(pastDate);
        expiredMeeting.setTeamCardId(UUID.randomUUID());
        expiredMeeting.setLink(null); // unfilled field

        meetingRepository.save(expiredMeeting);

        // when
        meetingStatusUpdateService.updateMeetingStatuses();

        // then
        var updatedMeeting = meetingRepository.findById(expiredMeeting.getId()).orElseThrow();
        Assertions.assertEquals(MeetingStatus.NOT_HAPPENED, updatedMeeting.getStatus());
    }

}