package net.akarmanov.projectplace.rest.api.meeting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Meeting API",
     description = "API для работы с встречами команды")
@RequestMapping("/api/v1/meetings")
public interface MeetingRestController {
  @Operation(summary = "Создание встречи команды")
  @PostMapping(consumes = "application/json",
               produces = "application/json")
  ResponseEntity<MeetingDto> createMeeting(
      @RequestParam UUID teamCardId,
      @Valid @RequestBody MeetingCreateDto meetingCreateDto);

  @Operation(summary = "Получение списка встреч команды")
  @GetMapping(produces = "application/json")
  PagedModel<MeetingDto> getMeetings(@RequestParam UUID teamCardId,
                                     @ParameterObject @PageableDefault Pageable pageable);

  @Operation(summary = "Обновление встречи команды")
  @PutMapping(value = "/{meetingId}",
              consumes = "application/json",
              produces = "application/json")
  ResponseEntity<MeetingDto> updateMeeting(@PathVariable UUID meetingId,
                                           @RequestParam UUID teamCardId,
                                           @Valid MeetingUpdateDto meetingCreateDto);

  @DeleteMapping(value = "/{meetingId}")
  @Operation(summary = "Удаление встречи команды")
  ResponseEntity<Void> deleteMeeting(@PathVariable UUID meetingId);
}
