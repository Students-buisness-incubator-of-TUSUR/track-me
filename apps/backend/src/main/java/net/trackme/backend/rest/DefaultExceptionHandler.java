package net.trackme.backend.rest;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.services.exceptions.PPNotFoundException;
import net.trackme.commons.filters.FilterFieldNotAllowedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.AccountLockedException;
import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class DefaultExceptionHandler {
  @ResponseBody
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ExceptionHandler({AuthenticationException.class, AccountLockedException.class})
  public ResponseEntity<RestError> handleAuthenticationException(Exception ex) {
    var restError = RestError.builder()
        .code(HttpStatus.UNAUTHORIZED.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(restError);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<RestError> handleConstraintViolationException(ConstraintViolationException ex) {
    var errors = ex.getConstraintViolations().stream()
        .map(violation -> violation.getPropertyPath().toString() + ": " + violation.getMessage())
        .toList();

    var restError = RestError.builder()
        .code(HttpStatus.BAD_REQUEST.toString())
        .message("Ошибка валидации запроса.")
        .errors(errors)
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restError);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RestError> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    var errors = ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .toList();

    var restError = RestError.builder()
        .code(HttpStatus.BAD_REQUEST.toString())
        .message("Ошибка валидации запроса.")
        .errors(errors)
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restError);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({FilterFieldNotAllowedException.class, IllegalArgumentException.class})
  public ResponseEntity<RestError> handleFilterFieldNotAllowedException(
          Exception ex) {
    var restError = RestError.builder()
        .code(HttpStatus.BAD_REQUEST.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restError);
  }


  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(PPNotFoundException.class)
  public ResponseEntity<RestError> handlePPNotFoundException(PPNotFoundException ex) {
    var restError = RestError.builder()
        .code(HttpStatus.NOT_FOUND.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(restError);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<RestError> handleAccessDeniedException(AccessDeniedException ex) {
    var restError = RestError.builder()
        .code(HttpStatus.FORBIDDEN.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(restError);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<RestError> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    var restError = RestError.builder()
        .code(HttpStatus.BAD_REQUEST.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restError);
  }
}
