package net.akarmanov.projectplace.repos;

import net.akarmanov.projectplace.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findByToken(String token);

  void deleteAllByExpiryDateBefore(Instant now);
}
