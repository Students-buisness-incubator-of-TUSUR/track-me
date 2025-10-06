package net.trackme.meetingservice.entities;

import lombok.Getter;

import java.util.Collection;

import static java.util.Arrays.asList;

@Getter
public enum MeetingStatus {
    SCHEDULED("Запланирована"),
    COMPLETED("Завершена"),
    NOT_HAPPENED("Не состоялась"),
    COMPLETED_AS_NOT_HAPPENED("Завершена как не состоявшаяся");

    private final String description;

    MeetingStatus(String description) {
        this.description = description;
    }

    public static Collection<MeetingStatus> completedStatuses() {
        return asList(COMPLETED, COMPLETED_AS_NOT_HAPPENED);
    }


    @Override
    public String toString() {
        return name();
    }
}
