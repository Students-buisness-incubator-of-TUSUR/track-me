package net.akarmanov.projectplace.services.tesk;

import net.akarmanov.projectplace.domain.Task;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;

import java.util.UUID;

public interface TaskService {
    TaskDto addTask(UUID meetingId, TaskCreateDto taskDto);

    TaskDto updateTask(TaskUpdateDto taskDto);

    void deleteTask(UUID taskId);

    void copyTask(UUID taskId, UUID meetingId);

    Task getTaskById(UUID taskId);
}
