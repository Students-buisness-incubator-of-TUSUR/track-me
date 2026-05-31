package net.trackme.sso.services.impl;

import net.trackme.sso.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Интеграционные тесты для клиента взаимодействия с backend-сервисом.
 * Проверяет обработку ошибок при недоступности backend.
 */
class BackendClientImplTest extends AbstractIntegrationTest {

    /** REST-клиент для взаимодействия с бэкендом. */
    @Autowired
    private RestClient restClient;

    /**
     * Тест получения команд пользователя при недоступном backend.
     * Ожидается исключение ResourceAccessException.
     */
    @Test
    void getUserTeamsBackendUnavailableThrowsException() {
        BackendClientImpl client = new BackendClientImpl(restClient);

        assertThrows(Exception.class,
                () -> client.getUserTeams("testuser", "test-token"));
    }

    /**
     * Тест переназначения команд при недоступном backend.
     * Ожидается исключение ResourceAccessException.
     */
    @Test
    void reassignTeamsToRoninBackendUnavailableThrowsException() {
        BackendClientImpl client = new BackendClientImpl(restClient);

        Map<String, String> request = Map.of(
                "fromUsername", "user1",
                "toUsername", "ronin",
                "toUserFullName", "Ronin User"
        );

        assertThrows(Exception.class,
                () -> client.reassignTeamsToRonin(request, "test-token"));
    }
}
