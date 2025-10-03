package net.trackme.meetingservice.services;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Exception thrown when a meeting image is not found.
 */
@ResponseStatus(code = NOT_FOUND, reason = "Meeting image not found")
public class MeetingImageNotFoundException extends RuntimeException {
    public MeetingImageNotFoundException(UUID meetingId) {
        super("Meeting image with ID " + meetingId + " not found.");
    }
}
