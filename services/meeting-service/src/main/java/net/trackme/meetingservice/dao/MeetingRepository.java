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
    List<Meeting> findByStatusAndStartDateBefore(MeetingStatus status, OffsetDateTime before,
                                                 Pageable pageable);

    boolean existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
            UUID teamCardId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    boolean existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThanAndIdNot(
            UUID teamCardId,
            OffsetDateTime from,
            OffsetDateTime to,
            UUID excludeId
    );

    @Query("""
    SELECT m FROM Meeting m\s
    WHERE m.startDate >= :dateAfter\s
      AND (
           m.status = 'COMPLETED_AS_NOT_HAPPENED'\s
        OR (m.status = 'SCHEDULED' AND m.startDate < :now)
      )
   \s""")
    List<Meeting> findMissedAndOverdueMeetings(
            @Param("dateAfter") OffsetDateTime dateAfter,
            @Param("now") OffsetDateTime now
    );
}
