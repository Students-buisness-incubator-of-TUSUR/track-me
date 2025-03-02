package net.akarmanov.projectplace.services.reset;

import net.akarmanov.projectplace.domain.User;

public interface PasswordResetService {
  void createToken(User user);

  void validateToken(String token);

  void resetPassword(String token, String newPassword);
}
