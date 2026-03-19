package net.trackme.backend.rest.api.teamcard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.commons.filters.FilterRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

@Tag(
        name = "Team Cards API",
        description = "API для работы с карточками команд")
@RequestMapping("/api/v1")
public interface TeamCardsRestController {
    @PostMapping(
            value = "team-card",
            consumes = "application/json",
            produces = "application/json")
    @Operation(
            summary = "Создание карточки команды",
            description = "Создает новую карточку команды",
            parameters = {
                    @Parameter(
                            name = "streamId",
                            description = "Идентификатор потока, к которому относится карточка команды")
            })
    ResponseEntity<TeamCardDto> createTeamCard(@RequestParam UUID streamId,
                                               @Valid @RequestBody
                                               TeamCardCreateDto teamCardDto,
                                               Authentication authentication);

    @PatchMapping(
            value = "team-card",
            consumes = "application/json",
            produces = "application/json")
    @Operation(summary = "Обновление карточки команды")
    ResponseEntity<TeamCardDto> updateTeamCard(@RequestParam UUID teamCardId,
                                               @Valid @RequestBody
                                               TeamCardUpdateDto teamCardDto);

    @PostMapping(
            value = "team-cards",
            produces = "application/json")
    @Operation(summary = "Получение списка карточек команд")
    ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(
            @Parameter(description = "Фильтры для поиска карточек команд")
            @RequestBody @Valid FilterRequest filters,
            @ParameterObject
            @PageableDefault
            Pageable pageable, Authentication authentication);

    @GetMapping(
            value = "team-card",
            produces = "application/json")
    @Operation(summary = "Получение карточки команды")
    ResponseEntity<TeamCardDto> getTeamCard(
            @Parameter(
                    description = "Идентификатор карточки команды",
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam
            UUID id);

    @GetMapping(value = "team-card/count")
    @Operation(summary = "Получение количества карточек команды по потоку")
    ResponseEntity<Integer> getTeamCardCount(
            @Parameter(description = "Идентификатор потока, к которому относится карточка команды")
            @RequestParam
            UUID streamId);

    @DeleteMapping(value = "team-card")
    @Operation(summary = "Удаление карточки команды")
    ResponseEntity<Void> deleteTeamCard(
            @Parameter(
                    description = "Идентификатор карточки команды",
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam
            UUID id);

    @PostMapping(
            value = "team-cards/reports",
            produces = "application/json")
    @Operation(summary = "Получение отчета о командах")
    ResponseEntity<PagedModel<TeamCardReportRecordDto>> getTeamCardReport(
            @Parameter(description = "Фильтры для поиска записей отчета")
            @RequestBody @Valid FilterRequest filters,
            @ParameterObject
            @PageableDefault
            Pageable pageable);

    @PostMapping(
            value = "team-cards/reports/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Получение отчета о командах в excel")
    ResponseEntity<StreamingResponseBody> getTeamCardReportExcel(
            @Parameter(description = "Фильтры для поиска записей отчета")
            @RequestBody @Valid FilterRequest filters);
}
