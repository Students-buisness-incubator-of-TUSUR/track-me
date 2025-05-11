package net.akarmanov.projectplace.services.meeting;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.repos.MeetingRepository;
import net.akarmanov.projectplace.services.acl.AclService;
import net.akarmanov.projectplace.services.exceptions.MeetingNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.MeetingSpecification.meetingIdEquals;
import static net.akarmanov.projectplace.domain.spec.MeetingSpecification.teamCardIdEquals;
import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;

    private final AclService aclService;

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#teamCard, 'READ') or hasRole('ADMIN')")
    public Meeting createMeeting(TeamCard teamCard, Meeting createMeeting) {
        createMeeting.setTeamCard(teamCard);
        var save = meetingRepository.save(createMeeting);
    var username = SecurityContextHolder.getContext().getAuthentication().getName();
    aclService.createAclForUserWithParent(save, username, teamCard);
    return save;
  }

    @Override
    @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'READ') " +
                  "or hasRole('ROLE_ADMIN')")
    public Page<Meeting> getMeetings(UUID teamCardId, Pageable pageable) {
        return meetingRepository.findAll(teamCardIdEquals(teamCardId), pageable);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'READ') or hasRole('ADMIN')")
    public Meeting updateMeeting(UUID meetingId, UUID teamCardId, Meeting updateMeeting) {
        var meeting = meetingRepository.findOne(where(teamCardIdEquals(teamCardId))
                        .and(meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId, teamCardId));
        updateEntity(meeting, updateMeeting);
        return meetingRepository.save(meeting);
    }

    private void updateEntity(Meeting meeting, Meeting createMeeting) {
        if (!Objects.equals(createMeeting.getLink(), meeting.getLink())) {
            meeting.setLink(createMeeting.getLink());
        }
        if (createMeeting.getStatus() != meeting.getStatus()) {
            meeting.setStatus(createMeeting.getStatus());
        }
        if (!Objects.equals(createMeeting.getNumber(), meeting.getNumber())) {
            meeting.setNumber(createMeeting.getNumber());
        }
        if (createMeeting.getTasksCurrentMeeting() != null) {
            meeting.setTasksCurrentMeeting(createMeeting.getTasksCurrentMeeting());
        }
        if (createMeeting.getTasksNextMeeting() != null) {
            meeting.setTasksNextMeeting(createMeeting.getTasksNextMeeting());
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#meetingId, 'net.akarmanov.projectplace.domain.Meeting', 'DELETE') or hasRole('ADMIN')")
    public void deleteMeeting(UUID meetingId) {
        var meeting = meetingRepository.findOne(where(meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        meetingRepository.delete(meeting);
        aclService.deleteAcl(meeting);
    }

}