package net.trackme.meetingservice.entities;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum MeetingStatus {
    SCHEDULED("Запланирована"),
    COMPLETED("Завершена"),
    COMPLETED_AS_NOT_HAPPENED("Завершена как не состоявшаяся");

    public static final List<MeetingStatus> COMPLETED_STATUSES = Arrays.asList(
            COMPLETED, COMPLETED_AS_NOT_HAPPENED);

    private final String description;

    MeetingStatus(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return name();
    }
}
