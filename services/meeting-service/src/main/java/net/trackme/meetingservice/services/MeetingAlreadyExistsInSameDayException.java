package net.trackme.meetingservice.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при попытке создать встречу в день,
 * на который уже запланирована другая встреча для данной карточки команды.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class MeetingAlreadyExistsInSameDayException extends RuntimeException {

    /**
     * @param message сообщение об ошибке
     */
    public MeetingAlreadyExistsInSameDayException(String message) {
        super(message);
    }
}
