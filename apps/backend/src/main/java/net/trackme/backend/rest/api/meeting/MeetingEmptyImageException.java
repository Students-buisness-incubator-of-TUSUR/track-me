package net.trackme.backend.rest.api.meeting;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.BAD_REQUEST,
        reason = "Image is empty")
public class MeetingEmptyImageException extends RuntimeException {
}
