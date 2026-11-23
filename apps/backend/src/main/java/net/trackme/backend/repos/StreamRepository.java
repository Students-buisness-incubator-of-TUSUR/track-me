package net.trackme.backend.repos;

import net.trackme.backend.domain.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StreamRepository extends JpaRepository<Stream, UUID>, JpaSpecificationExecutor<Stream> {

    List<Stream> findAllByOrganizationId(UUID organizationId);

    /**
     * Обновляет денормализованное название организации во всех её потоках.
     *
     * @param organizationId идентификатор организации
     * @param name           новое название
     * @return количество обновлённых потоков
     */
    @Modifying
    @Query("update Stream s set s.organizationName = :name where s.organizationId = :organizationId")
    int updateOrganizationName(@Param("organizationId") UUID organizationId,
                               @Param("name") String name);
}
