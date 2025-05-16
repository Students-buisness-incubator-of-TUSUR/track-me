package net.akarmanov.projectplace.services.meeting;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class MeetingMIMETypeException extends RuntimeException {
    public MeetingMIMETypeException(String contentType) {
        super("Invalid image MIME type: " + contentType);
    }
}