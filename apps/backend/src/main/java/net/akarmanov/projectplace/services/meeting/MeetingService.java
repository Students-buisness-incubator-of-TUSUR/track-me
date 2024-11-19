package net.akarmanov.projectplace.services.meeting;

import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.rest.api.meeting.MeetingCreateDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MeetingService {
    MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto meetingCreateDto);

    Page<MeetingDto> getMeetingsForCurrentUser(Pageable pageable);

    MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto meetingCreateDto);

    void deleteMeeting(UUID meetingId);

    Meeting getById(UUID targetId);
}
