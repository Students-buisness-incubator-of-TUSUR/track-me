package net.trackme.sso.exception;

public class WrongOldPasswordException extends RuntimeException {
  public WrongOldPasswordException(String s) {
    super(s);
  }
}
