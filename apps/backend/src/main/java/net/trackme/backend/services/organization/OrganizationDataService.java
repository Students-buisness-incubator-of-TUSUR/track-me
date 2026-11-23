package net.trackme.backend.services.organization;

import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.repos.StreamRepository;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.commons.acl.AclService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Сервис каскадного удаления данных организации в backend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationDataService {

  private final TeamCardsRepository teamCardsRepository;

  private final StreamRepository streamRepository;

  private final AclService aclService;

  /**
   * Удаляет все карточки команд и потоки указанной организации.
   *
   * @param organizationId идентификатор организации
   */
  @Transactional
  @CacheEvict(value = {"team-cards-report-all", "team-cards-report-page"}, allEntries = true)
  public void deleteOrganizationData(UUID organizationId) {
    var streams = streamRepository.findAllByOrganizationId(organizationId);
    var cards = new LinkedHashMap<UUID, TeamCard>();
    teamCardsRepository.findAllByOrganizationId(organizationId)
        .forEach(card -> cards.put(card.getId(), card));
    teamCardsRepository.findAllByStreamsOrganizationId(organizationId)
        .forEach(card -> cards.putIfAbsent(card.getId(), card));

    cards.values().forEach(this::deleteAclQuietly);
    teamCardsRepository.deleteAll(cards.values());
    teamCardsRepository.flush();
    streamRepository.deleteAll(streams);

    log.info("Удалено {} карточек команд и {} потоков организации {}",
        cards.size(), streams.size(), organizationId);
  }

  /**
   * Обновляет денормализованное название организации в потоках и карточках команд.
   *
   * @param organizationId идентификатор организации
   * @param organizationName новое название организации
   */
  @Transactional
  @CacheEvict(value = {"team-cards-report-all", "team-cards-report-page"}, allEntries = true)
  public void renameOrganization(UUID organizationId, String organizationName) {
    var updatedStreams = streamRepository.updateOrganizationName(
        organizationId, organizationName);
    var updatedCards = teamCardsRepository.updateOrganizationName(
        organizationId, organizationName);

    log.info("Название организации {} обновлено в {} потоках и {} карточках команд",
        organizationId, updatedStreams, updatedCards);
  }

  private void deleteAclQuietly(TeamCard teamCard) {
    try {
      aclService.deleteAcl(teamCard);
    } catch (Exception exception) {
      log.debug("ACL для карточки команды {} не найден: {}",
          teamCard.getId(), exception.getMessage());
    }
  }
}
