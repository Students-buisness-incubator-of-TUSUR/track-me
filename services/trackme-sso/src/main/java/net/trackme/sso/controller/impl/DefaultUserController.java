package net.trackme.sso.controller.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.controller.UserController;
import net.trackme.sso.dto.ErrorResponseDto;
import net.trackme.sso.dto.UserDto;
import net.trackme.sso.exception.TeamReassignmentException;
import net.trackme.sso.services.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> disableUser(String username) {
    log.info("Disabling user: {}", username);
    userService.disableUser(username);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> unlockUser(String username) {
    log.info("Unlocking user: {}", username);
    userService.unlockUser(username);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(String username) {
    log.info("Deleting user permanently: {}", username);
    userService.deleteUser(username);
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Map<String, String>>> getUserTeams(String username) {
    log.info("Getting teams for user: {}", username);
    List<Map<String, String>> teams = userService.getUserTeams(username);
    return ResponseEntity.ok(teams);
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

  @ExceptionHandler(TeamReassignmentException.class)
  public ResponseEntity<ErrorResponseDto> handleTeamReassignmentException(
          TeamReassignmentException e) {
      log.error("Team operation failed: {}", e.getMessage(), e);
      
      ErrorResponseDto errorResponse = ErrorResponseDto.builder()
          .error("TEAM_OPERATION_FAILED")
          .message(e.getMessage())
          .status(HttpStatus.CONFLICT.value())
          .timestamp(System.currentTimeMillis())
          .build();
      
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }
}
