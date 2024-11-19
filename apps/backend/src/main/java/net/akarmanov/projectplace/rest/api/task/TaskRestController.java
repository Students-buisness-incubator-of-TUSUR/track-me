package net.akarmanov.projectplace.rest.api.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/tasks")
@Tag(name = "Task API", description = "Задачи на встречи")
public interface TaskRestController {
    @Operation(summary = "Добавить задачу на встречу")
    @PostMapping(value = "/add", consumes = "application/json", produces = "application/json")
    ResponseEntity<TaskDto> addTask(@Valid @RequestBody TaskCreateDto taskDto, @RequestParam UUID meetingId);

    @Operation(summary = "Обновить информацию о задаче на встрече")
    @PostMapping(value = "/update", consumes = "application/json", produces = "application/json")
    ResponseEntity<TaskDto> updateTask(@Valid @RequestBody TaskUpdateDto taskDto);

    @Operation(summary = "Удалить задачу")
    @DeleteMapping(value = "/delete", produces = "application/json")
    ResponseEntity<Void> deleteTask(@RequestParam UUID taskId);

    @Operation(summary = "Копировать задачу на встречу")
    @PostMapping(value = "/copy", produces = "application/json")
    ResponseEntity<TaskDto> copyTask(@RequestParam UUID taskId, @RequestParam UUID meetingId);
}
