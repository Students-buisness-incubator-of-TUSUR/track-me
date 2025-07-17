package net.trackme.meetingservice.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.BAD_REQUEST,
        reason = "Image size is too large")
public class MeetingLargeImageSizeException extends RuntimeException {
    public MeetingLargeImageSizeException(long size) {
        super("Image size is too large: " + size + " bytes");
    }
}
