package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.api.MeetingCreateDto;
import net.trackme.meetingservice.api.MeetingDto;
import net.trackme.meetingservice.api.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingMapper meetingMapper;

    private final MeetingRepository meetingRepository;

    private final AclService aclService;

    @Override
    public MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto createDto) {
        var meeting = meetingMapper.mapToEntity(createDto);
        meeting.setTeamCardId(teamCardId);
        meeting = meetingRepository.save(meeting);
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        aclService.createAclForUserWithParent(meeting, username, teamCardId);
        return meetingMapper.mapToDto(meeting);
    }

    @Override
    public Page<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
        return null;
    }

    @Override
    public MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto updateDto) {
        return null;
    }

    @Override
    public void deleteMeeting(UUID meetingId) {
        // TODO: Implement delete logic
    }

    @Override
    public void addMeetingImage(UUID meetingId, MultipartFile file) {
        // TODO: Implement image upload logic
    }

    @Override
    public Resource getMeetingImage(UUID meetingId) {
        return null;
    }
}
