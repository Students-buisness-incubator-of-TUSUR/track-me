package net.akarmanov.projectplace.sso.exception;

import lombok.Getter;
import net.akarmanov.projectplace.sso.type.AuthErrorCode;
import org.springframework.security.core.AuthenticationException;

@Getter
public class AuthException extends AuthenticationException {
  private final AuthErrorCode errorCode;

  public AuthException(AuthErrorCode errorCode, String msg, Throwable cause) {
    super(msg, cause);
    this.errorCode = errorCode;
  }

  public AuthException(String msg, AuthErrorCode errorCode) {
    super(msg);
    this.errorCode = errorCode;
  }

  public AuthException(AuthErrorCode authErrorCode) {
    super(null);
    this.errorCode = authErrorCode;
  }
}
