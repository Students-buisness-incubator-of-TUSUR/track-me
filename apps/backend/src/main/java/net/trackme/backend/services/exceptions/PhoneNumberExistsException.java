package net.trackme.backend.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PhoneNumberExistsException extends RuntimeException {
  public PhoneNumberExistsException(String phoneNumber) {
    super("Пользователь с номером телефона " + phoneNumber + " уже существует");
  }
}
