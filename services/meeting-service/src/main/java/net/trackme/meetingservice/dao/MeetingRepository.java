package net.trackme.meetingservice.dao;

import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID>, JpaSpecificationExecutor<Meeting> {
    List<Meeting> findByStatusAndStartDateBefore(MeetingStatus status, OffsetDateTime before);

    List<Meeting> findByStatusAndStartDateBefore(MeetingStatus status, OffsetDateTime before,
                                                 Pageable pageable);
}
