package net.akarmanov.projectplace.sso.controller.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.commons.filters.FilterRequest;
import net.akarmanov.projectplace.sso.controller.UserController;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.services.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DefaultUserController implements UserController {

  private final UserService userService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> enableUser(String username) {
    log.info("Enabling user: {}", username);
    userService.enableUser(username);
    return null;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> disableUser(String username) {
    log.info("Disabling user: {}", username);
    userService.disableUser(username);
    return null;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDto> getUserInfo(String username) {
    log.info("Getting user info for: {}", username);
    UserDto userDto = userService.getUserInfo(username);
    return ResponseEntity.ok(userDto);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PagedModel<UserDto>> getTrackers(FilterRequest filterRequest,
                                                         Pageable pageable) {
    log.info("Getting trackers with filter: {}", filterRequest);
    var trackers = userService.getTrackers(filterRequest, pageable);
    return ResponseEntity.ok(new PagedModel<>(trackers));
  }

  @Override
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<PagedModel<UserDto>> getAdmins(FilterRequest filterRequest,
                                                       Pageable pageable) {
    log.info("Getting admins with filter: {}", filterRequest);
    var admins = userService.getAdmins(filterRequest, pageable);
    return ResponseEntity.ok(new PagedModel<>(admins));
  }
}
