package net.trackme.backend.repos;

import net.trackme.backend.domain.TeamCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TeamCardsRepository extends
        JpaRepository<TeamCard, UUID>,
        JpaSpecificationExecutor<TeamCard>,
        TeamCardsRepositoryCustom {
  Optional<TeamCard> findByIdAndUsername(UUID id, String username);

  void deleteByIdAndUsername(UUID id, String username);

  Integer countByStreamsIdIn(Collection<UUID> streams);

  List<TeamCard> findByUsername(String username);
}
