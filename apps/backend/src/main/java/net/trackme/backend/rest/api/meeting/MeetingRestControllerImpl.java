package net.trackme.backend.rest.api.meeting;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.usecases.TeamCardMeetingUseCase;
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

    private final TeamCardMeetingUseCase teamCardMeetingUseCase;

    @Override
    public ResponseEntity<MeetingDto> createMeeting(UUID teamCardId,
                                                    MeetingCreateDto meetingCreateDto) {
        var meeting = teamCardMeetingUseCase.createMeeting(teamCardId, meetingCreateDto);
        return ResponseEntity.ok(meeting);
    }

    @Override
    public PagedModel<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
        var meetings = teamCardMeetingUseCase.getMeetings(teamCardId, pageable);
        return new PagedModel<>(meetings);
    }

    @Override
    public ResponseEntity<MeetingDto> updateMeeting(UUID meetingId, UUID teamCardId,
                                                    MeetingUpdateDto meetingCreateDto) {
        var meeting = teamCardMeetingUseCase.updateMeeting(meetingId, teamCardId, meetingCreateDto);
        return ResponseEntity.ok(meeting);
    }

    @Override
    public ResponseEntity<Void> deleteMeeting(UUID meetingId) {
        teamCardMeetingUseCase.deleteMeeting(meetingId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addImage(UUID meetingId, MultipartFile file) {
        teamCardMeetingUseCase.addMeetingImage(meetingId, file);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> getImage(UUID meetingId) {
        var image = teamCardMeetingUseCase.getMeetingImage(meetingId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
