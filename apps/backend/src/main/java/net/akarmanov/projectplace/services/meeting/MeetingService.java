package net.akarmanov.projectplace.services.meeting;

import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MeetingService {
  Meeting createMeeting(TeamCard teamCard, Meeting createMeeting);

  Page<Meeting> getMeetingsForCurrentUser(Pageable pageable);

  Meeting updateMeeting(UUID meetingId, UUID teamCardId, Meeting createMeeting);

  void deleteMeeting(UUID meetingId);

  Meeting getById(UUID targetId);
}
