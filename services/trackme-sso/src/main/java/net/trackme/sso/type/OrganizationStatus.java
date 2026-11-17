package net.trackme.sso.type;

/**
 * Статус организации.
 */
public enum OrganizationStatus {
  /**
   * Организация активна: пользователи могут входить в систему.
   */
  ACTIVE,

  /**
   * Организация временно заблокирована владельцем платформы.
   */
  BLOCKED
}
