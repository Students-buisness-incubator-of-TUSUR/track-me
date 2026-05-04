package net.trackme.meetingservice.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class MeetingExcelReportException extends RuntimeException {
    public MeetingExcelReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
