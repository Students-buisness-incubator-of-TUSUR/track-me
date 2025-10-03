package net.trackme.meetingservice.services;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Исключение, которое выбрасывается, если изображение для встречи не найдено.
 */
@ResponseStatus(code = NOT_FOUND, reason = "Изображение для встречи не найдено")
public class MeetingImageNotFoundException extends RuntimeException {
    public MeetingImageNotFoundException(UUID meetingId) {
        super("Изображение для встречи с ID " + meetingId + " не найдено.");
    }
}
