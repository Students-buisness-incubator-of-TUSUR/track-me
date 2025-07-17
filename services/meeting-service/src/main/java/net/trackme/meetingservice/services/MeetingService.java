package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.MeetingCreateDto;
import net.trackme.meetingservice.api.MeetingDto;
import net.trackme.meetingservice.api.MeetingUpdateDto;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MeetingService {
    Integer MAX_FILE_SIZE = 100 * 1024 * 1024; // 100 MB

    MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto createDto);

    Page<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable);

    MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto updateDto);

    void deleteMeeting(UUID meetingId);

    void addMeetingImage(UUID meetingId, MultipartFile file);

    Resource getMeetingImage(UUID meetingId);
}
