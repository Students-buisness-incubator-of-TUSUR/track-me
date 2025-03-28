package net.akarmanov.projectplace.repos;

import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TeamCardsRepository extends JpaRepository<TeamCard, UUID>, JpaSpecificationExecutor<TeamCard> {
  Optional<TeamCard> findByIdAndUserId(UUID id, UUID userId);

  void deleteByIdAndUserId(UUID id, UUID userId);

  Integer countByStreamsIdIn(Collection<UUID> streams);
}
