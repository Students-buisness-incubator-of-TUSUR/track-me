package net.akarmanov.projectplace.rest.api.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.services.admin.AdministrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminTrackerRestControllerImpl implements AdministrationRestController {

  private final AdministrationService administrationService;

  @Override
  public ResponseEntity<Void> confirm(UUID userId) {
    administrationService.confirmUser(userId);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> unconfirm(UUID userId) {
    administrationService.unconfirmUser(userId);
    return ResponseEntity.ok().build();
  }

  @Override
  public Page<UserDTO> getTrackers(Pageable pageable) {
    return administrationService.getAllUsers(pageable);
  }

  @Override
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public Page<UserDTO> getAdministrators(Pageable pageable) {
    return null;
  }
}
