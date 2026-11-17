package net.trackme.sso.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Клиент для взаимодействия с backend-сервисом.
 */
public interface BackendClient {

    /**
     * Получить список команд пользователя.
     *
     * @param username    имя пользователя
     * @param bearerToken токен авторизации
     * @return список команд
     */
    List<Map<String, String>> getUserTeams(String username, String bearerToken);

    /**
     * Переназначить команды с одного пользователя на Ronin.
     *
     * @param request     данные запроса (fromUsername, toUsername, toUserFullName)
     * @param bearerToken токен авторизации
     */
    void reassignTeamsToRonin(Map<String, String> request, String bearerToken);

    /**
     * Удаляет данные организации в backend-сервисе.
     *
     * @param organizationId идентификатор организации
     * @param bearerToken    токен авторизации
     */
    void deleteOrganization(UUID organizationId, String bearerToken);

    /**
     * Обновляет название организации в данных backend-сервиса.
     *
     * @param organizationId идентификатор организации
     * @param name           новое название организации
     * @param bearerToken    токен авторизации
     */
    void renameOrganization(UUID organizationId, String name, String bearerToken);
}
