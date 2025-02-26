package net.akarmanov.projectplace.services.meeting;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.models.MeetingStatus;
import net.akarmanov.projectplace.repos.MeetingRepository;
import net.akarmanov.projectplace.services.acl.AclService;
import net.akarmanov.projectplace.services.exceptions.MeetingNotFoundException;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
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
import static net.akarmanov.projectplace.domain.spec.MeetingSpecification.userEquals;
import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository meetingRepository;

  private final TeamCardsService teamCardsService;

  private final AclService aclService;

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#teamCard, 'READ')")
  public Meeting createMeeting(TeamCard teamCard, Meeting createMeeting) {
    createMeeting.setTeamCard(teamCard);
    createMeeting.setStatus(MeetingStatus.OK);
    var save = meetingRepository.save(createMeeting);
    aclService.createAclWithParent(save, teamCard);
    return save;
  }

  @Override
  @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'READ') " +
                "or hasRole('ROLE_ADMIN')")
  public Page<Meeting> getMeetingsForCurrentUser(UUID teamCardId, Pageable pageable) {
    var username = SecurityContextHolder.getContext().getAuthentication().getName();
    return meetingRepository.findAll(where(userEquals(username))
        .and(teamCardIdEquals(teamCardId)), pageable);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'READ')")
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
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#meetingId, 'net.akarmanov.projectplace.domain.Meeting', 'DELETE')")
  public void deleteMeeting(UUID meetingId) {
    var meeting = meetingRepository.findOne(where(meetingIdEquals(meetingId)))
        .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    meetingRepository.delete(meeting);
  }

  @Override
  @PreAuthorize("hasPermission(#meetingId, 'net.akarmanov.projectplace.domain.Meeting', 'READ')")
  public Meeting getById(UUID meetingId) {
    return meetingRepository.findById(meetingId)
        .orElseThrow(() -> new MeetingNotFoundException(meetingId));
  }
}