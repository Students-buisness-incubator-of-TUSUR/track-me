package net.trackme.sso.controller.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.controller.OrganizationController;
import net.trackme.sso.dto.OrganizationCreateDto;
import net.trackme.sso.dto.OrganizationDto;
import net.trackme.sso.dto.OrganizationPublicDto;
import net.trackme.sso.dto.OrganizationUpdateDto;
import net.trackme.sso.services.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Реализация API управления организациями.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DefaultOrganizationController implements OrganizationController {

  private final OrganizationService organizationService;

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<List<OrganizationDto>> findAll() {
    return ResponseEntity.ok(organizationService.findAll());
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<OrganizationDto> get(UUID id) {
    return ResponseEntity.ok(organizationService.get(id));
  }

  @Override
  public ResponseEntity<List<OrganizationPublicDto>> findAvailable(String query) {
    return ResponseEntity.ok(organizationService.findAvailable(query));
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<OrganizationDto> create(OrganizationCreateDto createDto) {
    log.info("Creating organization '{}'", createDto.name());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(organizationService.create(createDto));
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<OrganizationDto> update(UUID id, OrganizationUpdateDto updateDto) {
    log.info("Updating organization {}", id);
    return ResponseEntity.ok(organizationService.update(id, updateDto));
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<Void> block(UUID id) {
    log.info("Blocking organization {}", id);
    organizationService.block(id);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<Void> unblock(UUID id) {
    log.info("Unblocking organization {}", id);
    organizationService.unblock(id);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<Void> delete(UUID id, String confirmation) {
    log.info("Deleting organization {} with confirmation", id);
    organizationService.delete(id, confirmation);
    return ResponseEntity.noContent().build();
  }
}
