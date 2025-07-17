package net.trackme.meetingservice.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Исключение, которое выбрасывается, если встреча не найдена.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Встреча не найдена")
public class MeetingNotFoundException extends RuntimeException {
    public MeetingNotFoundException(UUID meetingId, UUID teamCardId) {
        super("Встреча с id " + meetingId + " не найдена для карточки с id " + teamCardId);
    }

    public MeetingNotFoundException(UUID meetingId) {
        super("Встреча с id " + meetingId + " не найдена");
    }
}
