package net.trackme.meetingservice.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, которое выбрасывается, если тип содержимого изображения встречи не соответствует допустимым типам.
 */
@ResponseStatus(code = HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason = "Недопустимый тип содержимого")
public class MeetingMIMETypeException extends RuntimeException {
    public MeetingMIMETypeException(String contentType) {
        super("Недопустимый тип содержимого: " + contentType + ". Допустимые типы: image/jpeg, image/png.");
    }
}
