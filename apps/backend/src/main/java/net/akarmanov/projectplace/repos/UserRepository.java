package net.akarmanov.projectplace.repos;

import jakarta.validation.constraints.Size;
import net.akarmanov.projectplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  Optional<User> findByTelegramId(String telegramId);

  Optional<User> findByEmail(String email);

  boolean existsUserByTelegramId(String telegramId);

  boolean existsByTelegramIdOrPhoneNumber(@Size(max = 32) String telegramId,
                                          @Size(max = 32) String phoneNumber);

  boolean existsByPhoneNumber(@Size(max = 32) String phoneNumber);

}
