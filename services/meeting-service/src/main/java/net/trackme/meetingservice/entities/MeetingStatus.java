package net.trackme.meetingservice.entities;

import lombok.Getter;

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


    @Override
    public String toString() {
        return name();
    }
}
