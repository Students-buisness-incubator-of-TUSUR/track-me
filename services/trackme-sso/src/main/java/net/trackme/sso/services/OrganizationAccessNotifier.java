package net.trackme.sso.services;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.entity.UserEntity;
import org.springframework.stereotype.Service;

/**
 * Уведомляет пользователя о невозможности входа из-за организации.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationAccessNotifier {

  private final EmailService emailService;

  private final AppProperties appProperties;

  /**
   * Отправляет письмо о недоступности организации.
   *
   * @param user         пользователь
   * @param organization организация (может быть null)
   */
  public void notifyAccessDenied(UserEntity user, OrganizationEntity organization) {
    if (user.getEmail() == null || user.getEmail().isBlank()) {
      return;
    }
    try {
      var organizationName = organization == null ? "не назначена" : organization.getName();
      emailService.sendMail(
          user.getEmail(),
          appProperties.getMail().getFrom(),
          "[" + appProperties.getMail().getSubject() + "] Доступ ограничен",
          "email-organization-blocked.html",
          Map.of(
              "fullName", user.getFullName() == null ? user.getUsername() : user.getFullName(),
              "organizationName", organizationName,
              "appName", appProperties.getMail().getSubject(),
              "supportEmail", appProperties.getMail().getFrom()));
    } catch (Exception exception) {
      log.warn("Не удалось отправить уведомление о блокировке организации пользователю {}: {}",
          user.getUsername(), exception.getMessage());
    }
  }
}
