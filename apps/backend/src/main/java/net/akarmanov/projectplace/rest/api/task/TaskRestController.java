package net.akarmanov.projectplace.rest.api.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping("/api/v1/tasks")
@Tag(name = "Task API",
     description = "Задачи на встречи")
public interface TaskRestController {
  @Operation(summary = "Добавить задачу на встречу", deprecated = true)
  @PostMapping(value = "/add",
               consumes = "application/json",
               produces = "application/json")
  ResponseEntity<TaskDto> addTask(@Valid @RequestBody TaskCreateDto taskDto, @RequestParam UUID meetingId);

  @Operation(summary = "Обновить информацию о задаче на встрече", deprecated = true)
  @PostMapping(value = "/update",
               consumes = "application/json",
               produces = "application/json")
  ResponseEntity<TaskDto> updateTask(@Valid @RequestBody TaskUpdateDto taskDto);

  @Operation(summary = "Удалить задачу", deprecated = true)
  @DeleteMapping(value = "/delete",
                 produces = "application/json")
  ResponseEntity<Void> deleteTask(@RequestParam UUID taskId);

  @Operation(summary = "Копировать задачу на встречу", deprecated = true)
  @PostMapping(value = "/copy",
               produces = "application/json")
  ResponseEntity<TaskDto> copyTask(@RequestParam UUID taskId, @RequestParam UUID meetingId);
}
