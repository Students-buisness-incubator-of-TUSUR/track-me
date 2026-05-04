package net.trackme.meetingservice.services.integration.sso;

import net.trackme.meetingservice.services.integration.exceptions.IntegrationException;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;

import java.util.List;
import java.util.UUID;

/**
 * Клиент для взаимодействия с сервисом trackme-sso.
 */
public interface SsoApiClient {
    /**
     * Получает всех трекеров, существующих в системе.
     *
     * @return Список трекеров
     * @throws IntegrationException если сервис недоступен
     */
    List<UserDto> getTrackers();
}
