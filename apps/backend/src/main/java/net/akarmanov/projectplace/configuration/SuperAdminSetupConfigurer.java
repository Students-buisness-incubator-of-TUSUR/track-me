package net.akarmanov.projectplace.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Настройщики супер-администратора.
 * Выполняется при старте приложения.
 */
@RequiredArgsConstructor
public class SuperAdminSetupConfigurer {
  /**
   * Сервисы пользователей.
   */
  private final UserService userService;

  /**
   * Кодировщик паролей.
   */
  private final PasswordEncoder passwordEncoder;

  @Value("${app.superadmin.username}")
  private String superAdminUsername;

  @Value("${app.superadmin.password}")
  private String superAdminPassword;

  @Value("${app.superadmin.email}")
  private String getSuperAdminEmail;

  /**
   * Устанавливает супер-администратора при старте приложения.
   */
  @PostConstruct
  public void setupSuperAdmin() {
    if (!userService.existsByUsername(superAdminUsername)) {
      var superAdmin = User.builder()
          .telegramId(superAdminUsername)
          .password(passwordEncoder.encode(superAdminPassword))
          .role(UserRole.SUPER_ADMIN)
          .email(getSuperAdminEmail)
          .enabled(true)
          .build();
      userService.createUser(superAdmin);
    }
  }
}
