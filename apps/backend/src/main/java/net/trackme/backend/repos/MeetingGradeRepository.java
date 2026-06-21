package net.trackme.backend.repos;

import net.trackme.backend.domain.MeetingGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetingGradeRepository extends JpaRepository<MeetingGrade, UUID> {
    Optional<MeetingGrade> findByMeetingIdAndTeamCardId(UUID meetingId, UUID teamCardId);
}
