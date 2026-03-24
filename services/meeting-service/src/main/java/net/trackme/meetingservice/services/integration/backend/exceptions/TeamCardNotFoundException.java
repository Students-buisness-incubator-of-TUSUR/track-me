package net.trackme.meetingservice.services.integration.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(
        value = HttpStatus.NOT_FOUND,
        reason = "Team card not found")
public class TeamCardNotFoundException extends RuntimeException {

    public TeamCardNotFoundException() {
        super();
    }

    public TeamCardNotFoundException(UUID id) {
        super("Team card with ID " + id + " not found in backend service.");
    }
}