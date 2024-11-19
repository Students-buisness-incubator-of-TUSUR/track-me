package net.akarmanov.projectplace.services.meeting;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.domain.spec.MeetingSpecification;
import net.akarmanov.projectplace.models.MeetingStatus;
import net.akarmanov.projectplace.repos.MeetingRepository;
import net.akarmanov.projectplace.rest.api.meeting.MeetingCreateDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingUpdateDto;
import net.akarmanov.projectplace.services.exceptions.MeetingNotFoundException;
import net.akarmanov.projectplace.services.meeting.mapping.MeetingMapper;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.MeetingSpecification.*;
import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;

    private final TeamCardsService teamCardsService;

    private final UserService userService;

    private final MeetingMapper meetingMapper;

    @Override
    @Transactional
    public MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto meetingCreateDto) {
        var user = userService.getCurrentUser();
        var teamCard = teamCardsService.getTeamCardEntity(teamCardId, user.getId());
        var meeting = meetingMapper.mapToEntity(meetingCreateDto);

        meeting.setTeamCard(teamCard);
        meeting.setStatus(MeetingStatus.OK);

        meeting = meetingRepository.save(meeting);
        return meetingMapper.mapToDto(meeting);
    }

    @Override
    public Page<MeetingDto> getMeetingsForCurrentUser(Pageable pageable) {
        var user = userService.getCurrentUser();
        return meetingRepository.findAll(where(MeetingSpecification.userEquals(user.getId())), pageable)
                .map(meetingMapper::mapToDto);
    }

    @Override
    @Transactional
    public MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto meetingUpdateDto) {
        var user = userService.getCurrentUser();
        var meeting = meetingRepository.findOne(Specification.where(userEquals(user.getId()))
                        .and(MeetingSpecification.teamCardIdEquals(teamCardId)).and(MeetingSpecification.meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId, teamCardId));
        meetingMapper.updateEntity(meeting, meetingUpdateDto);
        meeting = meetingRepository.save(meeting);
        return meetingMapper.mapToDto(meeting);
    }

    @Override
    @Transactional
    public void deleteMeeting(UUID meetingId) {
        var user = userService.getCurrentUser();
        var meeting = meetingRepository.findOne(Specification.where(userEquals(user.getId()))
                        .and(meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        meetingRepository.delete(meeting);
    }

    @Override
    public Meeting getById(UUID targetId) {
        return meetingRepository.findById(targetId)
                .orElseThrow(() -> new MeetingNotFoundException(targetId));
    }
}