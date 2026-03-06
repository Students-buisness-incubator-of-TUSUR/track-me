package net.trackme.backend.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExcelExportException extends RuntimeException {
    public ExcelExportException(String message, Throwable cause) {
        super(message, cause);
    }
}