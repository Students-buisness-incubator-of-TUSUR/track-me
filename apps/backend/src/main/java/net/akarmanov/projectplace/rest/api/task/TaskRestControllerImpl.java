package net.akarmanov.projectplace.rest.api.task;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import net.akarmanov.projectplace.services.tesk.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskRestControllerImpl implements TaskRestController {

    private final TaskService taskService;

    @Override
    public ResponseEntity<TaskDto> addTask(TaskCreateDto taskDto, UUID meetingId) {
        var task = taskService.addTask(meetingId, taskDto);
        return ResponseEntity.ok(task);
    }

    @Override
    public ResponseEntity<TaskDto> updateTask(TaskUpdateDto taskDto) {
        var task = taskService.updateTask(taskDto);
        return ResponseEntity.ok(task);
    }

    @Override
    public ResponseEntity<Void> deleteTask(UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TaskDto> copyTask(UUID taskId, UUID meetingId) {
        taskService.copyTask(taskId, meetingId);
        return ResponseEntity.noContent().build();
    }
}
