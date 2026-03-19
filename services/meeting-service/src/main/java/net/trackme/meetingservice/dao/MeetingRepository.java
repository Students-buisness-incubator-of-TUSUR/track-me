package net.trackme.meetingservice.dao;

import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID>, JpaSpecificationExecutor<Meeting> {
    List<Meeting> findByStatusAndStartDateAfter(MeetingStatus status, OffsetDateTime after);

    List<Meeting> findByStatusAndStartDateBefore(MeetingStatus status, OffsetDateTime before);

    List<Meeting> findByStatusAndStartDateBefore(MeetingStatus status, OffsetDateTime before,
                                                 Pageable pageable);

    /**
     * Проверяет, существует ли встреча для указанной карточки команды
     * в заданном временном диапазоне, исключая встречу с указанным идентификатором.
     *
     * @param teamCardId идентификатор карточки команды
     * @param from       начало временного диапазона (включительно)
     * @param to         конец временного диапазона (исключительно)
     * @param excludeId  идентификатор встречи для исключения из проверки,
     *                   если {@code null} — исключение не применяется
     * @return {@code true} если встреча существует, {@code false} иначе
     */
    @Query(
            """
            SELECT COUNT(m) > 0 FROM Meeting m
            WHERE m.teamCardId = :teamCardId
              AND m.startDate >= :from
              AND m.startDate < :to
              AND (:excludeId IS NULL OR m.id <> :excludeId)
            """
    )
    boolean existsByTeamCardIdAndDateRangeExcluding(
            @Param("teamCardId") UUID teamCardId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("excludeId") UUID excludeId
    );
}
