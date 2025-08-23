package net.trackme.meetingservice.dao;

import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID>, JpaSpecificationExecutor<Meeting> {
    List<Meeting> findByStatusAndStartDateBefore(MeetingStatus status, OffsetDateTime before);

    @Query("SELECT m.teamStatus FROM Meeting m WHERE m.teamCardId = :teamCardId")
    List<TeamStatus> findTeamStatusByTeamCardId(UUID teamCardId);
}
