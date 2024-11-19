package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.Task;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TaskMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    TaskDto toDto(Task task);

    Task fromDto(TaskDto taskDto);
}
