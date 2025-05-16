package net.akarmanov.projectplace.services.meeting;

import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MeetingService {
  Integer MAX_FILE_SIZE = 100 * 1024 * 1024; // 100 MB

  Meeting createMeeting(TeamCard teamCard, Meeting createMeeting);

  Page<Meeting> getMeetings(UUID teamCardId, Pageable pageable);

  Meeting updateMeeting(UUID meetingId, UUID teamCardId, Meeting createMeeting);

  void deleteMeeting(UUID meetingId);

  void addImage(UUID meetingId, MultipartFile file);

  Meeting getById(UUID meetingId);
}
