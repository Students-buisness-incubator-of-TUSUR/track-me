package net.trackme.meetingservice.services.integration.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class IntegrationException extends RuntimeException {

    private final String serviceName;

    public IntegrationException(String serviceName) {
        super(String.format("Integration error: Service '%s' is currently unavailable or returned an error.", serviceName));
        this.serviceName = serviceName;
    }

    public IntegrationException(String serviceName, Throwable cause) {
        super(String.format("Integration error: Failed to connect to '%s' service.", serviceName), cause);
        this.serviceName = serviceName;
    }
}