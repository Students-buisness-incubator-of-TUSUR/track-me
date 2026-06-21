package net.trackme.sso.services;

import java.util.List;
import java.util.Map;

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
}
