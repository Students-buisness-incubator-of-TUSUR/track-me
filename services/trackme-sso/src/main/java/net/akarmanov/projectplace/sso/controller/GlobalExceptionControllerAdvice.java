package net.akarmanov.projectplace.sso.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.dto.ErrorResponseDto;
import net.akarmanov.projectplace.sso.exception.RegistrationException;
import net.akarmanov.projectplace.sso.exception.ServiceException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionControllerAdvice {

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

  @ExceptionHandler({ConstraintViolationException.class})
  public ResponseEntity<ErrorResponseDto> resolveConstraintViolation(
      HttpServletRequest request,
      ConstraintViolationException exception
  ) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("constraint.violation.exception")
            .message(exception.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDto> resolveIllegalArgument(
      HttpServletRequest request,
      IllegalArgumentException exception
  ) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("illegal.argument.exception")
            .message(exception.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> resolveMethodArgumentNotValid(
      HttpServletRequest request,
      MethodArgumentNotValidException exception
  ) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("method.argument.not.valid.exception")
            .message(exception.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(RegistrationException.class)
  public ResponseEntity<ErrorResponseDto> resolveRegistrationException(
      HttpServletRequest request,
      RegistrationException exception
  ) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("registration.exception")
            .message(exception.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ErrorResponseDto> resolveServiceException(
      HttpServletRequest request,
      ServiceException exception
  ) {
    logRequestException(request, exception);

    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .message(exception.getMessage())
            .error("service.exception")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> resolveGeneralException(
      HttpServletRequest request,
      Exception exception
  ) {
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
