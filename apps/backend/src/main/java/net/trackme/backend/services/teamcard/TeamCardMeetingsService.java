package net.trackme.backend.services.teamcard;

import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface TeamCardMeetingsService {
    void increaseMeetingCount(UUID teamCardId, UUID meetingId);

    void updateTeamCardInfo(UUID teamCardId, UUID uuid, MeetingStatus newStatus,
                            MeetingStatus oldStatus, TeamCardStatus teamCardStatus,
                            BigDecimal teamGrade, String meetingLink);
}
