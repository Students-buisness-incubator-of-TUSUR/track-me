package net.akarmanov.projectplace.usecases;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.mapping.TaskMapper;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import net.akarmanov.projectplace.services.tesk.TaskService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskUseCase {

  private final TaskService taskService;

  private final TaskMapper taskMapper;

  public TaskDto addTask(UUID meetingId, TaskCreateDto createDto) {
    var task = taskMapper.mapToEntity(createDto);
    var createdTask = taskService.addTask(meetingId, task);
    return taskMapper.mapToDto(createdTask);
  }

  public TaskDto updateTask(TaskUpdateDto updateDto) {
    var updateTask = taskMapper.mapToEntity(updateDto);
    var updatedTask = taskService.updateTask(updateTask);
    return taskMapper.mapToDto(updatedTask);
  }

  public void deleteTask(UUID taskId) {
    taskService.deleteTask(taskId);
  }

  public void copyTask(UUID taskId, UUID meetingId) {
    taskService.copyTask(taskId, meetingId);
  }
}
