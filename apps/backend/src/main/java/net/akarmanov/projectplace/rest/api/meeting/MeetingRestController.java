package net.akarmanov.projectplace.rest.api.meeting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Meeting API", description = "API для работы с встречами команды")
@RequestMapping("/api/v1/meetings")
public interface MeetingRestController {
    @Operation(summary = "Создание встречи команды")
    @PostMapping(value = "/create", consumes = "application/json", produces = "application/json")
    ResponseEntity<MeetingDto> createMeeting(
            @RequestParam UUID teamCardId,
            @Valid @RequestBody MeetingCreateDto meetingCreateDto);

    @Operation(summary = "Получение списка встреч команды")
    @PostMapping(value = "/list", produces = "application/json")
    ResponseEntity<PagedModel<MeetingDto>> getMeetings(@ParameterObject @PageableDefault Pageable pageable);

    @Operation(summary = "Обновление встречи команды")
    @PostMapping(value = "/{meetingId}/update", consumes = "application/json", produces = "application/json")
    ResponseEntity<MeetingDto> updateMeeting(@PathVariable UUID meetingId,
                                             @RequestParam UUID teamCardId,
                                             @Valid MeetingUpdateDto meetingCreateDto);

    @DeleteMapping(value = "/{meetingId}/delete")
    @Operation(summary = "Удаление встречи команды")
    ResponseEntity<Void> deleteMeeting(@PathVariable UUID meetingId);
}
