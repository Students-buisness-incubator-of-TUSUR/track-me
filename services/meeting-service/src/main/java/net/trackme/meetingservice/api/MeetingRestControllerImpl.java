package net.trackme.meetingservice.api;

import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.services.MeetingService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MeetingRestControllerImpl implements MeetingRestController {

    private final MeetingService meetingService;

    @Override
    public ResponseEntity<MeetingDto> createMeeting(UUID teamCardId,
                                                    MeetingCreateDto meetingCreateDto) {
        var meeting = meetingService.createMeeting(teamCardId, meetingCreateDto);
        return ResponseEntity.ok(meeting);
    }

    @Override
    public PagedModel<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
        var meetings = meetingService.getMeetings(teamCardId, pageable);
        return new PagedModel<>(meetings);
    }

    @Override
    public ResponseEntity<MeetingDto> updateMeeting(UUID meetingId, UUID teamCardId,
                                                    MeetingUpdateDto meetingCreateDto) {
        var meeting = meetingService.updateMeeting(meetingId, teamCardId, meetingCreateDto);
        return ResponseEntity.ok(meeting);
    }

    @Override
    public ResponseEntity<Void> deleteMeeting(UUID meetingId) {
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addImage(UUID meetingId, MultipartFile file) {
        meetingService.addMeetingImage(meetingId, file);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> getImage(UUID meetingId) {
        var image = meetingService.getMeetingImage(meetingId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

}
