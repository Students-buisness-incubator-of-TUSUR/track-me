package net.akarmanov.projectplace.sso.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.dto.ErrorResponseDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({NoResultException.class, EmptyResultDataAccessException.class})
  public ResponseEntity<ErrorResponseDto> resolveNoResult(HttpServletRequest request,
                                                          Exception exception) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("no.result.exception")
            .message(exception.getMessage())
            .status(HttpStatus.NOT_FOUND.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler({EntityNotFoundException.class})
  public ResponseEntity<ErrorResponseDto> resolveEntityNotFound(HttpServletRequest request,
                                                                Exception exception) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("entity.not.found.exception")
            .message(exception.getMessage())
            .status(HttpStatus.NOT_FOUND.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> resolveAccessDeniedException(
      HttpServletRequest request,
      Exception exception
  ) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("access.denied.exception")
            .message(exception.getMessage())
            .status(HttpStatus.FORBIDDEN.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.FORBIDDEN
    );
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponseDto> resolveException(HttpServletRequest request,
                                                              Exception exception) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("internal.server.error")
            .message(exception.getMessage())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  private void logRequestException(HttpServletRequest request, Exception exception) {
    log.debug("Unexpected exception processing request: {}", request.getRequestURI());
    log.error("Exception: ", exception);
  }
}
