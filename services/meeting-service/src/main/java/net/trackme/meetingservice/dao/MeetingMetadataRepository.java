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
     * Находит уникальные идентификаторы карточек команд, для которых существуют встречи
     * с незаполненными или поврежденными метаданными (NULL-значения в денормализованных полях).
     *
     * @return список уникальных UUID карточек команд, требующих восстановления данных.
     */
    @Query("""
        SELECT DISTINCT m.teamCardId FROM Meeting m
        WHERE m.teamCardId IS NOT NULL\s
          AND (
               m.teamName IS NULL
            OR m.trackerUsername IS NULL
            OR m.trackerId IS NULL
            OR m.trackerFullName IS NULL
            OR m.teamStatusValue IS NULL
            OR m.streamIds IS EMPTY
          )
   \s""")
    List<UUID> findTeamIdsWithIncompleteMetadata();

    /**
     * Находит все встречи конкретной команды, которые имеют незаполненные метаданные.
     *
     * @param teamId идентификатор карточки команды.
     * @return список сущностей {@link Meeting} с неполными данными для указанной команды.
     */
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.teamCardId = :teamId
          AND (
               m.teamName IS NULL\s
            OR m.trackerUsername IS NULL\s
            OR m.trackerId IS NULL
            OR m.trackerFullName IS NULL\s
            OR m.teamStatusValue IS NULL
            OR m.streamIds IS EMPTY
          )
   \s""")
    List<Meeting> findAllIncompleteByTeamCardId(@Param("teamId") UUID teamId);

    /**
     * Возвращает список всех уникальных идентификаторов карточек команд,
     * записи о которых присутствуют в таблице встреч.
     *
     * @return список уникальных UUID всех команд.
     */
    @Query("SELECT DISTINCT m.teamCardId FROM Meeting m WHERE m.teamCardId IS NOT NULL")
    List<UUID> findAllUniqueTeamCardIds();

    /**
     * Находит абсолютно все встречи, связанные с указанной карточкой команды.
     *
     * @param teamCardId идентификатор карточки команды.
     * @return список всех встреч данной команды.
     */
    List<Meeting> findAllByTeamCardId(UUID teamCardId);

    /**
     * Выполняет массовое обновление денормализованных полей встреч для конкретной команды.
     * Использует логику COALESCE, чтобы предотвратить перезапись существующих данных значениями NULL.
     *
     * @param teamId      идентификатор команды, чьи встречи подлежат обновлению.
     * @param newName     новое название команды.
     * @param newUsername новый логин трекера.
     * @param newTrackerId новый внутренний идентификатор трекера.
     * @param newFullName  новое полное имя трекера.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Meeting m
        SET m.teamName = COALESCE(:newName, m.teamName),
            m.trackerUsername = COALESCE(:newUsername, m.trackerUsername),
            m.trackerId = COALESCE(:newTrackerId, m.trackerId),
            m.trackerFullName = COALESCE(:newFullName, m.trackerFullName)
        WHERE m.teamCardId = :teamId
    """)
    void updateMetadata(
            @Param("teamId") UUID teamId,
            @Param("newName") String newName,
            @Param("newUsername") String newUsername,
            @Param("newTrackerId") String newTrackerId,
            @Param("newFullName") String newFullName
    );

    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO meeting_stream (meeting_id, stream_id)
        SELECT id, :streamId FROM meeting WHERE team_card_id = :teamId
        ON CONFLICT DO NOTHING
    """, nativeQuery = true)
    void addStreamToTeamMeetings(@Param("teamId") UUID teamId, @Param("streamId") UUID streamId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        DELETE FROM meeting_stream 
        WHERE stream_id = :streamId 
        AND meeting_id IN (SELECT id FROM meeting WHERE team_card_id = :teamId)
    """, nativeQuery = true)
    void removeStreamFromTeamMeetings(@Param("teamId") UUID teamId, @Param("streamId") UUID streamId);

    /**
     * Массово обновляет ФИО трекера во всех встречах, где указан соответствующий username.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Meeting m SET m.trackerFullName = :fullName WHERE m.trackerUsername = :username")
    void updateTrackerFullNameByUsername(@Param("username") String username, @Param("fullName") String fullName);
}