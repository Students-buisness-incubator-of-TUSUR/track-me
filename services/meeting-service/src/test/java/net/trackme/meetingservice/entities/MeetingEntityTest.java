package net.trackme.meetingservice.entities;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
class MeetingEntityTest {

    @Test
    void updateTeamStatusValueSkipWhenTeamCardPassiveIsTrue() {
        var meeting = Meeting.builder()
                .teamCardPassive(true)
                .status(MeetingStatus.COMPLETED)
                .teamStatus(TeamStatus.OK)
                .build();

        meeting.updateTeamStatusValue();

        // Значение не должно обновиться (останется null или 0)
        assertThat(meeting.getTeamStatusValue()).isNull();
    }

    @Test
    void updateTeamStatusValueCalculatesWhenTeamCardPassiveIsFalse() {
        var meeting = Meeting.builder()
                .teamCardPassive(false)
                .status(MeetingStatus.COMPLETED)
                .teamStatus(TeamStatus.OK)
                .build();

        meeting.updateTeamStatusValue();

        assertThat(meeting.getTeamStatusValue()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
