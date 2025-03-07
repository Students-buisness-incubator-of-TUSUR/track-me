package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.Meeting;
import net.akarmanov.projectplace.rest.api.meeting.MeetingCreateDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingDto;
import net.akarmanov.projectplace.rest.api.meeting.MeetingUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    uses = {
        TaskMapper.class,
    })
public interface
MeetingMapper {
  @Mapping(target = "teamCard",
           ignore = true)
  @Mapping(target = "status",
           ignore = true)
  @Mapping(target = "screenshot",
           ignore = true)
  @Mapping(target = "meetingTasks",
           ignore = true)
  @Mapping(target = "id",
           ignore = true)
  Meeting mapToEntity(MeetingCreateDto meetingCreateDto);

  @Mapping(target = "tasks",
           source = "meetingTasks")
  @Mapping(target = "teamCardId",
           source = "teamCard.id")
  MeetingDto mapToDto(Meeting meeting);

  @Mapping(target = "teamCard",
           ignore = true)
  @Mapping(target = "startDate",
           ignore = true)
  @Mapping(target = "screenshot",
           ignore = true)
  @Mapping(target = "meetingTasks",
           ignore = true)
  @Mapping(target = "id",
           ignore = true)
  void updateEntity(@MappingTarget Meeting meeting, MeetingUpdateDto meetingCreateDto);

  @Mapping(target = "teamCard",
           ignore = true)
  @Mapping(target = "startDate",
           ignore = true)
  @Mapping(target = "screenshot",
           ignore = true)
  @Mapping(target = "meetingTasks",
           ignore = true)
  @Mapping(target = "id",
           ignore = true)
  Meeting mapToEntity(MeetingUpdateDto meetingCreateDto);
}
