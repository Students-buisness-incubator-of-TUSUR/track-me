package net.trackme.meetingservice.entities;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;

@Getter
public enum MeetingStatus {
    SCHEDULED("Запланирована"),
    COMPLETED("Завершена"),
    FINALLY_COMPLETED("Окончательно завершена"),
    COMPLETED_AS_NOT_HAPPENED("Завершена как не состоявшаяся");

    public static final List<MeetingStatus> COMPLETED_STATUSES = Arrays.asList(
            COMPLETED, FINALLY_COMPLETED, COMPLETED_AS_NOT_HAPPENED);

    private final String description;

    MeetingStatus(String description) {
        this.description = description;
    }

    /**
     * Проверяет, может ли суперадминистратор редактировать встречу с данным статусом.
     * @return true если статус FINALLY_COMPLETED или COMPLETED_AS_NOT_HAPPENED
     */
    public boolean isEditableBySuperAdmin() {
        return this == FINALLY_COMPLETED || this == COMPLETED_AS_NOT_HAPPENED;
    }

    @Override
    public String toString() {
        return name();
    }
}
