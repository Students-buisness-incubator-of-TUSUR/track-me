package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.api.MeetingCreateDto;
import net.trackme.meetingservice.api.MeetingDto;
import net.trackme.meetingservice.api.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.events.MeetingCreatedEvent;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static net.trackme.meetingservice.entities.MeetingSpecification.meetingIdEquals;
import static net.trackme.meetingservice.entities.MeetingSpecification.teamCardIdEquals;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingMapper meetingMapper;

    private final MeetingRepository meetingRepository;

    private final AclService aclService;

    private final MeetingEventsProducer meetingEventsProducer;

    @Override
    @Transactional
    public MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto createDto) {
        var meeting = meetingMapper.mapToEntity(createDto);
        meeting.setTeamCardId(teamCardId);
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting = meetingRepository.save(meeting);
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        aclService.createAclForUser(meeting, username);
        sendMeetingCreatedEvent(meeting);
        return meetingMapper.mapToDto(meeting);
    }

    @Override
    public Page<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
        var meetings = meetingRepository.findAll(teamCardIdEquals(teamCardId), pageable);
        return meetings.map(meetingMapper::mapToDto);
    }

    @Override
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting','READ') or hasRole('ADMIN')")
    public MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto updateDto) {
        var meeting = meetingRepository.findOne(teamCardIdEquals(teamCardId)
                        .and(meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId, teamCardId));
        if (MeetingStatus.COMPLETED_STATUSES.contains(meeting.getStatus())) {
            throw new MeetingCompletedException(meetingId, teamCardId);
        }
        meetingMapper.updateEntityFromDto(updateDto, meeting);
        meeting = meetingRepository.save(meeting);
        return meetingMapper.mapToDto(meeting);
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'READ') or hasRole('ADMIN')")
    public void deleteMeeting(UUID meetingId) {
        var meeting = meetingRepository.getReferenceById(meetingId);
        meetingRepository.delete(meeting);
        aclService.deleteAcl(meeting);
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'WRITE') or hasRole('ADMIN')")
    public void addMeetingImage(UUID meetingId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new MeetingEmptyImageException();
        }

        if (file.getSize() > MeetingService.MAX_FILE_SIZE) {
            throw new MeetingLargeImageSizeException(file.getSize());
        }

        String contentType = file.getContentType();
        if (!MediaType.IMAGE_PNG_VALUE.equals(contentType) &&
            !MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            throw new MeetingMIMETypeException(contentType);
        }

        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            if (!ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg")) {
                throw new MeetingImageExtensionException(ext);
            }
        }

        var meeting = meetingRepository.getReferenceById(meetingId);
        try {
            meeting.setImageBytes(file.getBytes());
            meetingRepository.save(meeting);
        } catch (Exception e) {
            throw new MeetingImageUploadException(meetingId, e);
        }
    }

    @Override
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'READ') or hasRole('ADMIN')")
    public Resource getMeetingImage(UUID meetingId) {
        var meeting = getMeeting(meetingId);
        if (meeting.getImageBytes() == null) {
            throw new MeetingImageNotFoundException(meetingId);
        }
        return new ByteArrayResource(meeting.getImageBytes());
    }

    private Meeting getMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    private void sendMeetingCreatedEvent(Meeting meeting) {
        var event = MeetingCreatedEvent.builder()
                .meetingId(meeting.getId())
                .teamCardId(meeting.getTeamCardId())
                .build();
        meetingEventsProducer.sendMeetingCreatedEvent(event);
    }
}
