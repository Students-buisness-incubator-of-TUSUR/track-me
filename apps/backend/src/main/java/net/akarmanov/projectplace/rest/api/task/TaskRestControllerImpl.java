package net.akarmanov.projectplace.rest.api.task;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import net.akarmanov.projectplace.usecases.TaskUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskRestControllerImpl implements TaskRestController {

  private final TaskUseCase taskUseCase;

  @Override
  public ResponseEntity<TaskDto> addTask(TaskCreateDto taskDto, UUID meetingId) {
    var task = taskUseCase.addTask(meetingId, taskDto);
    return ResponseEntity.ok(task);
  }

  @Override
  public ResponseEntity<TaskDto> updateTask(TaskUpdateDto taskDto) {
    var task = taskUseCase.updateTask(taskDto);
    return ResponseEntity.ok(task);
  }

  @Override
  public ResponseEntity<Void> deleteTask(UUID taskId) {
    taskUseCase.deleteTask(taskId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<TaskDto> copyTask(UUID taskId, UUID meetingId) {
    taskUseCase.copyTask(taskId, meetingId);
    return ResponseEntity.noContent().build();
  }
}
