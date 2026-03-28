package net.trackme.meetingservice.dao;
import net.trackme.meetingservice.entities.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления денормализованными метаданными встреч.
 */
@Repository
public interface MeetingMetadataRepository extends JpaRepository<Meeting, UUID> {

    /**
     * Находит уникальные ID команд, у которых есть встречи с неполными метаданными.
     * Используется для инициализации процесса миграции данных.
     */
    @Query("""
        SELECT DISTINCT m.teamCardId FROM Meeting m
        WHERE m.teamName IS NULL
           OR m.trackerUsername IS NULL
           OR m.teamStatusValue IS NULL
    """)
    List<UUID> findTeamIdsWithIncompleteMetadata();

    /**
     * Находит встречи конкретной команды, требующие заполнения метаданных.
     */
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.teamCardId = :teamId
          AND (m.teamName IS NULL OR m.trackerUsername IS NULL OR m.teamStatusValue IS NULL)
    """)
    List<Meeting> findAllIncompleteByTeamCardId(@Param("teamId") UUID teamId);

    /**
     * Массово обновляет метаданные встреч при изменении данных в основном сервисе.
     * Параметры обновляются только если они не null (логика COALESCE).
     */
    @Modifying
    @Query("""
        UPDATE Meeting m
        SET m.teamName = COALESCE(:newName, m.teamName),
            m.trackerUsername = COALESCE(:newUsername, m.trackerUsername)
        WHERE m.teamCardId = :teamId
    """)
    void updateMetadata(
            @Param("teamId") UUID teamId,
            @Param("newName") String newName,
            @Param("newUsername") String newUsername
    );
}