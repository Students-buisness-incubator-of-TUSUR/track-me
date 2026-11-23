package net.trackme.sso.services;

import java.util.UUID;

/**
 * Клиент для взаимодействия с meeting-сервисом.
 */
public interface MeetingClient {

  /**
   * Удаляет данные организации в meeting-сервисе.
   *
   * @param organizationId идентификатор организации
   * @param bearerToken    токен авторизации
   */
  void deleteOrganization(UUID organizationId, String bearerToken);

  /**
   * Обновляет название организации во встречах meeting-сервиса.
   *
   * @param organizationId идентификатор организации
   * @param name           новое название организации
   * @param bearerToken    токен авторизации
   */
  void renameOrganization(UUID organizationId, String name, String bearerToken);
}
