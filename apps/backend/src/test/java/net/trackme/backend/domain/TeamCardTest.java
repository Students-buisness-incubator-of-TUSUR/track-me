package net.trackme.backend.domain;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamCardTest {

    @Test
    void getMeetingsCountPlan_shouldReturnMeetingsCountFromStream() {
        // Arrange
        var stream = Stream.builder()
                .name("Test Stream")
                .meetingsCount(5)
                .build();

        var teamCard = TeamCard.builder()
                .name("Test Team")
                .streams(Set.of(stream))
                .build();

        // Act
        Integer result = teamCard.getMeetingsCountPlan();

        // Assert
        assertEquals(5, result);
    }

    @Test
    void getMeetingsCountPlan_shouldReturnZeroWhenNoStream() {
        // Arrange
        var teamCard = TeamCard.builder()
                .name("Test Team")
                .streams(Set.of())
                .build();

        // Act
        Integer result = teamCard.getMeetingsCountPlan();

        // Assert
        assertEquals(0, result);
    }
}