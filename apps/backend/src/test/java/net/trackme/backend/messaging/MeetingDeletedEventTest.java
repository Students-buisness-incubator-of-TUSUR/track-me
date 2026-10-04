package net.trackme.backend.messaging;

import net.trackme.backend.models.MeetingStatus;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingDeletedEventTest {

    @Test
    void shouldCreateMeetingDeletedEvent() {
        UUID meetingId = UUID.randomUUID();
        UUID teamCardId = UUID.randomUUID();
        OffsetDateTime startDate = OffsetDateTime.now();
        MeetingStatus status = MeetingStatus.COMPLETED;

        MeetingDeletedEvent event = new MeetingDeletedEvent(meetingId, teamCardId, startDate, status);

        assertThat(event.meetingId()).isEqualTo(meetingId);
        assertThat(event.teamCardId()).isEqualTo(teamCardId);
        assertThat(event.startDate()).isEqualTo(startDate);
        assertThat(event.status()).isEqualTo(status);
    }
}
