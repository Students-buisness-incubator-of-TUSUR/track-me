package net.trackme.backend.repos;

import net.trackme.backend.domain.TeamCard;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  List<TeamCard> findByUsernameAndOrganizationId(String username, UUID organizationId);

  List<TeamCard> findAllByOrganizationId(UUID organizationId);

  List<TeamCard> findAllByStreamsOrganizationId(UUID organizationId);

  Slice<TeamCard> findAllBy(Pageable pageable);

  /**
   * Обновляет денормализованное название организации во всех карточках команд.
   *
   * @param organizationId идентификатор организации
   * @param name           новое название
   * @return количество обновлённых карточек
   */
  @Modifying
  @Query("update TeamCard t set t.organizationName = :name where t.organizationId = :organizationId")
  int updateOrganizationName(@Param("organizationId") UUID organizationId,
                             @Param("name") String name);
}
