package net.trackme.sso.services.impl;

import java.util.UUID;
import net.trackme.sso.services.MeetingClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Реализация клиента meeting-сервиса.
 */
@Service
public class MeetingClientImpl implements MeetingClient {

  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final RestClient restClient;

  /**
   * Создаёт клиент meeting-сервиса.
   *
   * @param restClient HTTP-клиент meeting-сервиса
   */
  public MeetingClientImpl(@Qualifier("meetingRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public void deleteOrganization(UUID organizationId, String bearerToken) {
    restClient.delete()
        .uri("/api/v1/admin/organizations/{organizationId}", organizationId)
        .header(AUTH_HEADER, BEARER_PREFIX + bearerToken)
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public void renameOrganization(UUID organizationId, String name, String bearerToken) {
    restClient.patch()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/admin/organizations/{organizationId}/name")
            .queryParam("name", name)
            .build(organizationId))
        .header(AUTH_HEADER, BEARER_PREFIX + bearerToken)
        .retrieve()
        .toBodilessEntity();
  }
}
