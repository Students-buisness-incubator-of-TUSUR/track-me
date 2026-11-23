package net.trackme.sso.services.impl;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.repository.OrganizationRepository;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.OrganizationCreateDto;
import net.trackme.sso.dto.OrganizationDto;
import net.trackme.sso.dto.OrganizationPublicDto;
import net.trackme.sso.dto.OrganizationUpdateDto;
import net.trackme.sso.services.BackendClient;
import net.trackme.sso.services.CurrentUserService;
import net.trackme.sso.services.MeetingClient;
import net.trackme.sso.services.OrganizationService;
import net.trackme.sso.type.OrganizationStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса управления организациями.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

  private static final String ORGANIZATION_NOT_FOUND =
      "Организация с идентификатором %s не найдена.";
  private static final String ORGANIZATION_NOT_AVAILABLE =
      "Организация недоступна для регистрации и входа.";
  private static final String DEFAULT_ORGANIZATION_DELETE =
      "Организацию по умолчанию удалить нельзя.";
  private static final String CONFIRMATION_MISMATCH =
      "Для удаления организации необходимо подтвердить её название.";

  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;
  private final BackendClient backendClient;
  private final MeetingClient meetingClient;
  private final CurrentUserService currentUserService;

  @Override
  @Transactional
  public OrganizationDto create(OrganizationCreateDto createDto) {
    var name = normalizeName(createDto.name());
    if (organizationRepository.existsByNameIgnoreCase(name)) {
      throw new IllegalArgumentException(
          "Организация с названием '" + name + "' уже существует.");
    }
    var organization = new OrganizationEntity();
    organization.setName(name);
    organization.setExpirationDate(createDto.expirationDate());
    organization.setStatus(OrganizationStatus.ACTIVE);
    return toDto(organizationRepository.save(organization));
  }

  @Override
  @Transactional
  public OrganizationDto update(UUID id, OrganizationUpdateDto updateDto) {
    var organization = findEntity(id);
    var name = normalizeName(updateDto.name());
    organizationRepository.findByNameIgnoreCase(name)
        .filter(found -> !found.getId().equals(id))
        .ifPresent(found -> {
          throw new IllegalArgumentException(
              "Организация с названием '" + name + "' уже существует.");
        });
    var previousName = organization.getName();
    organization.setName(name);
    organization.setExpirationDate(updateDto.expirationDate());
    var saved = organizationRepository.save(organization);
    if (!previousName.equals(name)) {
      renameExternalData(id, name);
    }
    return toDto(saved);
  }

  @Override
  public List<OrganizationDto> findAll() {
    return organizationRepository.findAllByOrderByNameAsc().stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  public OrganizationDto get(UUID id) {
    return toDto(findEntity(id));
  }

  @Override
  public List<OrganizationPublicDto> findAvailable(String query) {
    var organizations = query == null || query.isBlank()
        ? organizationRepository.findAllByOrderByNameAsc()
        : organizationRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
    var today = LocalDate.now();
    return organizations.stream()
        .filter(organization -> organization.isAvailable(today))
        .map(organization -> new OrganizationPublicDto(
            organization.getId().toString(), organization.getName()))
        .toList();
  }

  @Override
  @Transactional
  public void block(UUID id) {
    var organization = findEntity(id);
    organization.setStatus(OrganizationStatus.BLOCKED);
    organizationRepository.save(organization);
  }

  @Override
  @Transactional
  public void unblock(UUID id) {
    var organization = findEntity(id);
    organization.setStatus(OrganizationStatus.ACTIVE);
    organizationRepository.save(organization);
  }

  @Override
  @Transactional
  public void delete(UUID id, String confirmation) {
    if (DEFAULT_ORGANIZATION_ID.equals(id)) {
      throw new AccessDeniedException(DEFAULT_ORGANIZATION_DELETE);
    }
    var organization = findEntity(id);
    if (confirmation == null
        || !organization.getName().equalsIgnoreCase(confirmation.trim())) {
      throw new IllegalArgumentException(CONFIRMATION_MISMATCH);
    }

    var bearerToken = currentUserService.getBearerToken();
    deleteExternalData(id, bearerToken);

    var users = userRepository.findAllByOrganizationId(id);
    if (!users.isEmpty()) {
      userRepository.deleteAll(users);
    }
    organizationRepository.delete(organization);
    log.info("Организация '{}' ({}) удалена вместе с {} пользователями",
        organization.getName(), id, users.size());
  }

  @Override
  public boolean isAvailable(UUID id) {
    return organizationRepository.findById(id)
        .map(organization -> organization.isAvailable(LocalDate.now()))
        .orElse(false);
  }

  @Override
  public OrganizationEntity getAvailableOrganization(UUID id) {
    var organization = findEntity(id);
    if (!organization.isAvailable(LocalDate.now())) {
      throw new AccessDeniedException(ORGANIZATION_NOT_AVAILABLE);
    }
    return organization;
  }

  @Override
  @Transactional
  public int blockExpired(LocalDate today) {
    var expired = organizationRepository.findByStatusAndExpirationDateBefore(
        OrganizationStatus.ACTIVE, today);
    if (expired.isEmpty()) {
      return 0;
    }
    expired.forEach(organization -> organization.setStatus(OrganizationStatus.BLOCKED));
    organizationRepository.saveAll(expired);
    log.info("Заблокировано {} организаций с истёкшим сроком действия", expired.size());
    return expired.size();
  }

  private OrganizationEntity findEntity(UUID id) {
    return organizationRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            String.format(ORGANIZATION_NOT_FOUND, id)));
  }

  private String normalizeName(String name) {
    return name == null ? "" : name.trim();
  }

  private OrganizationDto toDto(OrganizationEntity organization) {
    return OrganizationDto.builder()
        .id(organization.getId().toString())
        .name(organization.getName())
        .status(organization.getStatus())
        .expirationDate(organization.getExpirationDate())
        .expired(organization.isExpired(LocalDate.now()))
        .build();
  }

  private void renameExternalData(UUID organizationId, String name) {
    var bearerToken = currentUserService.getBearerToken();
    var token = bearerToken == null ? "" : bearerToken;
    try {
      backendClient.renameOrganization(organizationId, name, token);
    } catch (Exception exception) {
      log.warn("Не удалось обновить название организации {} в backend: {}",
          organizationId, exception.getMessage());
    }
    try {
      meetingClient.renameOrganization(organizationId, name, token);
    } catch (Exception exception) {
      log.warn("Не удалось обновить название организации {} в meeting-service: {}",
          organizationId, exception.getMessage());
    }
  }

  private void deleteExternalData(UUID organizationId, String bearerToken) {
    try {
      backendClient.deleteOrganization(organizationId, bearerToken == null ? "" : bearerToken);
    } catch (Exception exception) {
      log.warn("Не удалось удалить данные организации {} в backend: {}",
          organizationId, exception.getMessage());
    }
    try {
      meetingClient.deleteOrganization(organizationId, bearerToken == null ? "" : bearerToken);
    } catch (Exception exception) {
      log.warn("Не удалось удалить данные организации {} в meeting-service: {}",
          organizationId, exception.getMessage());
    }
  }
}
