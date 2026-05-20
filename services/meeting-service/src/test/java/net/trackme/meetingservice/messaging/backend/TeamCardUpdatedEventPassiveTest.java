package net.trackme.meetingservice.messaging.backend;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TeamCardUpdatedEventPassiveTest {

    @Test
    void newPassiveFieldShouldBePresent() {
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = true;

        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                teamId, "Name", "user", newPassive, "FullName"
        );

        assertThat(event.newPassive()).isTrue();
    }

    @Test
    void newPassiveCanBeFalse() {
        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                UUID.randomUUID(), "Name", "user", false, null
        );
        assertThat(event.newPassive()).isFalse();
    }

    @Test
    void newPassiveCanBeNull() {
        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                UUID.randomUUID(), "Name", "user", null, null
        );
        assertThat(event.newPassive()).isNull();
    }
}