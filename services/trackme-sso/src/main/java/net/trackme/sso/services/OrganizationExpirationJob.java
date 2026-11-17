package net.trackme.sso.services;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически блокирует организации с истёкшим сроком действия.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationExpirationJob {

  private final OrganizationService organizationService;

  /**
   * Проверяет сроки действия организаций.
   */
  @Scheduled(cron = "${app.organization.expiration-cron:0 0 1 * * *}")
  public void blockExpiredOrganizations() {
    var blocked = organizationService.blockExpired(LocalDate.now());
    if (blocked > 0) {
      log.info("Автоматически заблокировано организаций: {}", blocked);
    }
  }
}
