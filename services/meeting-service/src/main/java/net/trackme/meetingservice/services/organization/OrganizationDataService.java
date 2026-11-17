package net.trackme.meetingservice.services.organization;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис каскадного удаления данных организации в meeting-сервисе.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationDataService {

  private final MeetingRepository meetingRepository;

  private final MeetingMetadataRepository meetingMetadataRepository;

  private final AclService aclService;

  /**
   * Удаляет встречи организации, их связи с потоками и ACL-записи.
   *
   * @param organizationId идентификатор организации
   */
  @Transactional
  @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
  public void deleteOrganizationData(UUID organizationId) {
    var meetings = meetingRepository.findAllByOrganizationId(organizationId);

    meetings.forEach(this::deleteAclQuietly);

    meetingMetadataRepository.deleteMeetingStreamsByOrganizationId(organizationId);
    meetingMetadataRepository.deleteAllByOrganizationId(organizationId);

    log.info("Удалено {} встреч организации {}", meetings.size(), organizationId);
  }

  /**
   * Обновляет денормализованное название организации во встречах.
   *
   * @param organizationId идентификатор организации
   * @param organizationName новое название организации
   */
  @Transactional
  @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
  public void renameOrganization(UUID organizationId, String organizationName) {
    meetingMetadataRepository.updateOrganizationName(organizationId, organizationName);
    log.info("Название организации {} обновлено во встречах", organizationId);
  }

  private void deleteAclQuietly(Meeting meeting) {
    try {
      aclService.deleteAcl(meeting);
    } catch (Exception exception) {
      log.debug("ACL для встречи {} не найден: {}", meeting.getId(), exception.getMessage());
    }
  }
}
