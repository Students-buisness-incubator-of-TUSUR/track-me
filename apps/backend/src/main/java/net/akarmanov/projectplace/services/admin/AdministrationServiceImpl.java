package net.akarmanov.projectplace.services.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.filters.Filter;
import net.akarmanov.projectplace.mapping.UserMapper;
import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.UserSpecification.byRole;
import static net.akarmanov.projectplace.domain.spec.UserSpecification.withFilters;

@Service
@RequiredArgsConstructor
class AdministrationServiceImpl implements AdministrationService {

  private final UserService userService;

  private final UserMapper userMapper;

  @Override
  public void confirmUser(UUID userId) {
    userService.enableUser(userId);
  }

  @Override
  public void unconfirmUser(UUID userId) {
    userService.disableUser(userId);
  }

  @Override
  public Page<UserDTO> getAllAdmins(@Valid @NotNull List<Filter> filters, Pageable pageable) {
    return userService.findAll(withFilters(filters).and(byRole(UserRole.ADMIN)), pageable)
        .map(userMapper::mapUserToDto);
  }

  @Override
  public Page<UserDTO> getAllTrackers(@Valid @NotNull List<Filter> filters, Pageable pageable) {
    return userService.findAll(withFilters(filters).and(byRole(UserRole.TRACKER)), pageable)
        .map(userMapper::mapUserToDto);
  }
}
