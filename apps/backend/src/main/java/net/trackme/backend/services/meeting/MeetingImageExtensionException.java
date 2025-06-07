package net.trackme.backend.services.meeting;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class MeetingImageExtensionException extends RuntimeException {
    public MeetingImageExtensionException(String ext) {
        super("Invalid image extension: " + ext);
    }
}
