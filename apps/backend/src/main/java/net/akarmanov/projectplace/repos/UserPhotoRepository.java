package net.akarmanov.projectplace.repos;

import jakarta.validation.constraints.NotNull;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.domain.UserPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserPhotoRepository extends JpaRepository<UserPhoto, UUID> {

  Optional<UserPhoto> findByUser(@NotNull User user);

  Optional<UserPhoto> findByUserId(@NotNull UUID userId);

  void deleteByUserId(UUID userId);

  @Query("SELECT up FROM UserPhoto up WHERE up.user.telegramId = :telegramId")
  Optional<UserPhoto> findByTelegramId(String telegramId);
}
