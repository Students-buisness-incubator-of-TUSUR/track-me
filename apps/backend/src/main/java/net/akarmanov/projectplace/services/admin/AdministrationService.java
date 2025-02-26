package net.akarmanov.projectplace.services.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.akarmanov.projectplace.filters.Filter;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdministrationService {
  void confirmUser(UUID userId);

  void unconfirmUser(UUID userId);

  Page<UserDTO> getAllAdmins(List<Filter> filters, Pageable pageable);

  Page<UserDTO> getAllTrackers(@Valid @NotNull List<Filter> filters, Pageable pageable);

  UserDTO getUserInfo(UUID userId);
}
