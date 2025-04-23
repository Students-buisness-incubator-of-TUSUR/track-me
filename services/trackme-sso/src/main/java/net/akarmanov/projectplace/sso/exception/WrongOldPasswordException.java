package net.akarmanov.projectplace.sso.exception;

public class WrongOldPasswordException extends RuntimeException {
  public WrongOldPasswordException(String s) {
    super(s);
  }
}
