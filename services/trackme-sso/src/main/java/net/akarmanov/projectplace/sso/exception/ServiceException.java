package net.akarmanov.projectplace.sso.exception;

public class ServiceException extends RuntimeException {
  public ServiceException(String message, Throwable ex) {
    super(message, ex);
  }
}
