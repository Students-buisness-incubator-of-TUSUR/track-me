package net.akarmanov.projectplace.rest;

import jakarta.validation.ConstraintViolationException;
import net.akarmanov.projectplace.services.exceptions.PPNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class DefaultExceptionHandler {
  @ResponseBody
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<RestError> handleAuthenticationException(AuthenticationException ex) {
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
  public ResponseEntity<RestError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
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

  // обработчик ошибок фильтрации сущностей JPA Specification
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<RestError> handleIllegalArgumentException(IllegalArgumentException ex) {
    var restError = RestError.builder()
        .code(HttpStatus.BAD_REQUEST.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restError);
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<RestError> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    var restError = RestError.builder()
        .code(HttpStatus.BAD_REQUEST.toString())
        .message(ex.getMessage())
        .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restError);
  }
}
