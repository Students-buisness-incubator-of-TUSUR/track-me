package net.akarmanov.projectplace.services.admin;

import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdministrationService {
  void confirmUser(UUID userId);

  void unconfirmUser(UUID userId);

  Page<UserDTO> getAllUsers(Pageable pageable);
}
