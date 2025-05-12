package net.akarmanov.projectplace.usecases;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.mapping.MeetingMapper;
import net.akarmanov.projectplace.rest.api.meeting.MeetingCreateDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingUpdateDto;
import net.akarmanov.projectplace.services.meeting.MeetingService;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TeamCardMeetingUseCase {

  private final MeetingService meetingService;

  private final TeamCardsService teamCardsService;

  private final MeetingMapper meetingMapper;

  public MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto meetingCreateDto) {
    var createMeeting = meetingMapper.mapToEntity(meetingCreateDto);
    var teamCard = teamCardsService.getTeamCard(teamCardId);
    var createdMeeting = meetingService.createMeeting(teamCard, createMeeting);
    return meetingMapper.mapToDto(createdMeeting);
  }

  public Page<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
    var meetings = meetingService.getMeetings(teamCardId, pageable);
    return meetings.map(meetingMapper::mapToDto);
  }

  public MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto meetingCreateDto) {
    var updateMeeting = meetingMapper.mapToEntity(meetingCreateDto);
    var updatedMeeting = meetingService.updateMeeting(meetingId, teamCardId, updateMeeting);
    return meetingMapper.mapToDto(updatedMeeting);
  }

  public void deleteMeeting(UUID meetingId) {
    meetingService.deleteMeeting(meetingId);
  }
}
