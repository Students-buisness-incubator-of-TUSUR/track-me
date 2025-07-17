package net.trackme.meetingservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(
        name = "Meeting API",
        description = "API для работы с встречами команды")
@RequestMapping("/api/v1")
public interface MeetingRestController {

    @Operation(summary = "Создание встречи команды")
    @PostMapping(
            value = "/create-meeting",
            consumes = "application/json",
            produces = "application/json")
    ResponseEntity<MeetingDto> createMeeting(
            @RequestParam UUID teamCardId,
            @Valid @RequestBody MeetingCreateDto meetingCreateDto);

    @Operation(summary = "Получение списка встреч команды")
    @GetMapping(
            produces = "application/json",
            value = "/meetings")
    PagedModel<MeetingDto> getMeetings(@RequestParam UUID teamCardId,
                                       @ParameterObject @PageableDefault Pageable pageable);

    @Operation(summary = "Обновление встречи команды")
    @PatchMapping(
            value = "update-meeting/{meetingId}",
            consumes = "application/json",
            produces = "application/json")
    ResponseEntity<MeetingDto> updateMeeting(@PathVariable UUID meetingId,
                                             @RequestParam UUID teamCardId,
                                             @Valid @RequestBody MeetingUpdateDto meetingCreateDto);

    @DeleteMapping(value = "delete-meeting/{meetingId}")
    @Operation(summary = "Удаление встречи команды")
    ResponseEntity<Void> deleteMeeting(@PathVariable UUID meetingId);

    @Operation(summary = "Добавление изображения к встрече")
    @PostMapping(
            value = "image/{meetingId}",
            consumes = "multipart/form-data")
    ResponseEntity<Void> addImage(@PathVariable UUID meetingId, @RequestParam MultipartFile file);

    @Operation(summary = "Получение изображения встречи")
    @GetMapping(
            value = "image/{meetingId}",
            produces = "image/png")
    ResponseEntity<Resource> getImage(@PathVariable UUID meetingId);
}
