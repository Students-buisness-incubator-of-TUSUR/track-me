package net.trackme.meetingservice.entities;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MeetingPassiveFlagTest {

    @Test
    void teamCardPassiveDefaultIsFalse() {
        Meeting meeting = Meeting.builder().build();
        assertThat(meeting.getTeamCardPassive()).isFalse();
    }

    @Test
    void teamCardPassiveCanBeSetToTrue() {
        Meeting meeting = Meeting.builder().teamCardPassive(true).build();
        assertThat(meeting.getTeamCardPassive()).isTrue();
    }

    @Test
    void updateTeamStatusValue_shouldSkipWhenTeamCardPassiveTrue() {
        Meeting meeting = Meeting.builder()
                .teamCardPassive(true)
                .status(MeetingStatus.COMPLETED)
                .teamStatus(TeamStatus.OK)
                .build();

        meeting.updateTeamStatusValue();

        assertThat(meeting.getTeamStatusValue()).isNull();
    }

    @Test
    void updateTeamStatusValue_shouldCalculateWhenTeamCardPassiveFalse() {
        Meeting meeting = Meeting.builder()
                .teamCardPassive(false)
                .status(MeetingStatus.COMPLETED)
                .teamStatus(TeamStatus.OK)
                .build();

        meeting.updateTeamStatusValue();

        assertThat(meeting.getTeamStatusValue()).isNotNull();
    }
}
