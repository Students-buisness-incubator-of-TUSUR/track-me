package net.trackme.meetingservice.services.exceptions;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(code = BAD_REQUEST)
public class MeetingCompletedException extends RuntimeException {
    public MeetingCompletedException(UUID meetingId, UUID teamCardId) {
        super("Meeting with id " + meetingId + " under team card with id " + teamCardId + " is already completed and cannot be modified.");
    }
}
