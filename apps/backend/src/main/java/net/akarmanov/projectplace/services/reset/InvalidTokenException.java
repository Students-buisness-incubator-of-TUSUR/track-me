package net.akarmanov.projectplace.services.reset;

public class InvalidTokenException extends RuntimeException {
  public InvalidTokenException(String token) {
    super("Невалидный токен: " + token);
  }
}
