package net.trackme.sso.services.impl;

import net.trackme.sso.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BackendClientImplTest extends AbstractIntegrationTest {

    @Autowired
    private RestClient restClient;

    @Test
    void getUserTeams_backendUnavailable_throwsException() {
        BackendClientImpl client = new BackendClientImpl(restClient);
        
        // Backend недоступен на localhost:9999 — выбросит ResourceAccessException
        assertThrows(Exception.class, 
            () -> client.getUserTeams("testuser", "test-token"));
    }

    @Test
    void reassignTeamsToRonin_backendUnavailable_throwsException() {
        BackendClientImpl client = new BackendClientImpl(restClient);
        
        Map<String, String> request = Map.of(
            "fromUsername", "user1",
            "toUsername", "ronin",
            "toUserFullName", "Ronin User"
        );
        
        // Backend недоступен на localhost:9999 — выбросит ResourceAccessException
        assertThrows(Exception.class, 
            () -> client.reassignTeamsToRonin(request, "test-token"));
    }
}
