package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.Task;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TaskMapper {
  @Mapping(target = "meetingId",
           source = "meeting.id")
  TaskDto mapToDto(Task task);

  @Mapping(target = "meeting",
           ignore = true)
  Task mapToEntity(TaskDto taskDto);

  @Mapping(target = "status",
           ignore = true)
  @Mapping(target = "number",
           ignore = true)
  @Mapping(target = "meeting",
           ignore = true)
  @Mapping(target = "id",
           ignore = true)
  Task mapToEntity(TaskCreateDto createDto);

  @Mapping(target = "number",
           ignore = true)
  @Mapping(target = "meeting",
           ignore = true)
  Task mapToEntity(TaskUpdateDto updateDto);
}
