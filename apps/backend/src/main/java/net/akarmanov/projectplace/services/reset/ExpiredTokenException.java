package net.akarmanov.projectplace.services.reset;

public class ExpiredTokenException extends RuntimeException {
  public ExpiredTokenException(String token) {
    super("Токен истек: " + token);
  }
}
