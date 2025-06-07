package net.trackme.sso.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.commons.filters.FilterFieldNotAllowedException;
import net.trackme.sso.dto.ErrorResponseDto;
import net.trackme.sso.exception.ConfirmRegistrationException;
import net.trackme.sso.exception.RegistrationException;
import net.trackme.sso.exception.ServiceException;
import net.trackme.sso.exception.WrongOldPasswordException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionControllerAdvice {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(value = {NoResultException.class, EmptyResultDataAccessException.class, UsernameNotFoundException.class},
                    produces = "application/json")
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

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(value = {EntityNotFoundException.class}, produces = "application/json")
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

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(value = AccessDeniedException.class, produces = "application/json")
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

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = {ConstraintViolationException.class}, produces = "application/json")
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

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = IllegalArgumentException.class, produces = "application/json")
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

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = WrongOldPasswordException.class, produces = "application/json")
  public ResponseEntity<ErrorResponseDto> resolveWrongOldPassword(
      HttpServletRequest request,
      WrongOldPasswordException exception) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("wrong.old.password.exception")
            .message(exception.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.BAD_REQUEST
    );
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = MethodArgumentNotValidException.class, produces = "application/json")
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

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = RegistrationException.class, produces = "application/json")
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

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = ConfirmRegistrationException.class, produces = "application/json")
  public ResponseEntity<ErrorResponseDto> resolveConfirmRegistrationException(
      HttpServletRequest request,
      ConfirmRegistrationException exception) {
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

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(value = FilterFieldNotAllowedException.class, produces = "application/json")
  public ResponseEntity<ErrorResponseDto> resolveFilterFieldNotAllowedException(
      HttpServletRequest request,
      FilterFieldNotAllowedException exception
  ) {
    logRequestException(request, exception);
    return new ResponseEntity<>(
        ErrorResponseDto.builder()
            .error("filter.field.not.allowed.exception")
            .message(exception.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(System.currentTimeMillis())
            .build(),
        HttpStatus.BAD_REQUEST
    );
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(value = ServiceException.class, produces = "application/json")
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

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(value = Exception.class, produces = "application/json")
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
