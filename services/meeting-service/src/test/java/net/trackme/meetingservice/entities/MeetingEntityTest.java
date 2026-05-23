package net.trackme.meetingservice.entities;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MeetingTest {

    @Test
    void updateTeamStatusValue_whenPassive_shouldNotChangeStatusValue() {
        // Arrange
        Meeting meeting = Meeting.builder()
                .teamCardPassive(true)
                .teamStatus(TeamStatus.OK) // даже если OK, не должно измениться
                .teamStatusValue(BigDecimal.valueOf(999)) // какое-то старое значение
                .build();

        // Act
        meeting.updateTeamStatusValue();

        // Assert
        assertEquals(BigDecimal.valueOf(999), meeting.getTeamStatusValue());
    }

    @Test
    void updateTeamStatusValue_whenNotPassive_shouldUpdateBasedOnStatus() {
        // Arrange
        Meeting meeting = Meeting.builder()
                .teamCardPassive(false)
                .teamStatus(TeamStatus.OK)
                .build();

        // Act
        meeting.updateTeamStatusValue();

        // Assert
        assertEquals(BigDecimal.valueOf(1.0), meeting.getTeamStatusValue());
    }

    @Test
    void updateTeamStatusValue_whenStatusCompletedAsNotHappened_shouldSetMinusOneEvenIfPassiveIsFalse() {
        // Проверяем приоритет: статус COMPLETED_AS_NOT_HAPPENED важнее passive
        Meeting meeting = Meeting.builder()
                .teamCardPassive(false)
                .status(MeetingStatus.COMPLETED_AS_NOT_HAPPENED)
                .build();

        meeting.updateTeamStatusValue();

        assertEquals(BigDecimal.valueOf(-1.0), meeting.getTeamStatusValue());
    }
}