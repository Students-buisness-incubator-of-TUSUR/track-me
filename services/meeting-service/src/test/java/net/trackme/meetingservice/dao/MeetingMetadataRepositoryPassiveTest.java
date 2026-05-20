package net.trackme.meetingservice.dao;

import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class MeetingMetadataRepositoryPassiveTest extends AbstractIntegrationTest {

    @Autowired
    private MeetingMetadataRepository meetingMetadataRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Test
    @Transactional
    void updatePassiveFlag_shouldUpdateTeamCardPassiveForAllMeetings() {
        // Arrange
        UUID teamCardId = UUID.randomUUID();

        Meeting meeting1 = Meeting.builder()
                .teamCardId(teamCardId)
                .teamCardPassive(false)
                .status(MeetingStatus.SCHEDULED)
                .startDate(OffsetDateTime.now().plusDays(1))
                .number("1")
                .build();

        Meeting meeting2 = Meeting.builder()
                .teamCardId(teamCardId)
                .teamCardPassive(false)
                .status(MeetingStatus.SCHEDULED)
                .startDate(OffsetDateTime.now().plusDays(2))
                .number("2")
                .build();

        meetingRepository.save(meeting1);
        meetingRepository.save(meeting2);

        // Act
        meetingMetadataRepository.updatePassiveFlag(teamCardId, true);

        // Assert
        Meeting updatedMeeting1 = meetingRepository.findById(meeting1.getId()).orElseThrow();
        Meeting updatedMeeting2 = meetingRepository.findById(meeting2.getId()).orElseThrow();

        assertThat(updatedMeeting1.getTeamCardPassive()).isTrue();
        assertThat(updatedMeeting2.getTeamCardPassive()).isTrue();
    }

    @Test
    @Transactional
    void updatePassiveFlag_shouldSetFalseWhenPassedFalse() {
        // Arrange
        UUID teamCardId = UUID.randomUUID();

        Meeting meeting = Meeting.builder()
                .teamCardId(teamCardId)
                .teamCardPassive(true)
                .status(MeetingStatus.SCHEDULED)
                .startDate(OffsetDateTime.now().plusDays(1))
                .number("1")
                .build();

        meetingRepository.save(meeting);

        // Act
        meetingMetadataRepository.updatePassiveFlag(teamCardId, false);

        // Assert
        Meeting updatedMeeting = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertThat(updatedMeeting.getTeamCardPassive()).isFalse();
    }
}