package net.trackme.meetingservice.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Исключение, которое выбрасывается при ошибке загрузки изображения встречи.
 */
@ResponseStatus(
        code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Ошибка загрузки изображения встречи")
public class MeetingImageUploadException extends RuntimeException {
    public MeetingImageUploadException(UUID meetingId, Exception e) {
        super("Ошибка загрузки изображения встречи: " + meetingId, e);
    }
}
