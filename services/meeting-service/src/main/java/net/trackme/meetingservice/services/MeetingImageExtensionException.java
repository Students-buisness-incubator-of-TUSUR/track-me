package net.trackme.meetingservice.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, которое выбрасывается, если расширение изображения встречи не соответствует допустимым расширениям.
 */
@ResponseStatus(
        code = HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason = "Недопустимое расширение изображения")
public class MeetingImageExtensionException extends RuntimeException {
    public MeetingImageExtensionException(String ext) {
        super("Недопустимое расширение изображения: " + ext + ". Допустимые расширения: jpg, jpeg, png.");
    }
}
