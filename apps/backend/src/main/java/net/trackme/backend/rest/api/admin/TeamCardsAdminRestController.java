package net.trackme.backend.rest.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.commons.filters.FilterRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Map;

@Tag(name = "Team Cards API",
     description = "API для работы с карточками команд для администратора")
@RequestMapping("/api/v1/admin")
@Validated
public interface TeamCardsAdminRestController {
  
  /**
   * DTO для запроса переназначения команд.
   */
  record ReassignTeamsRequest(
      @NotBlank(message = "fromUsername не может быть пусто")
      String fromUsername,
      
      @NotBlank(message = "toUsername не может быть пусто")
      String toUsername,
      
      String toUserFullName
  ) {}

  @PostMapping(value = "team-card",
               consumes = "application/json",
               produces = "application/json")
  @Operation(summary = "Создание карточки команды")
  ResponseEntity<TeamCardDto> createTeamCard(
      @Valid @RequestBody TeamCardCreateDto teamCardDto,
      @RequestParam(required = false) UUID streamId,
      @RequestParam String username);

  @PatchMapping(value = "team-card",
                consumes = "application/json",
                produces = "application/json")
  @Operation(summary = "Обновление карточки команды")
  ResponseEntity<TeamCardDto> updateTeamCard(@RequestParam UUID teamCardId,
                                             @RequestParam(required = false) String username,
                                             @RequestParam(required = false) UUID streamId,
                                             @Valid @RequestBody TeamCardUpdateDto teamCardDto);

  @PostMapping(value = "team-cards",
               produces = "application/json")
  @Operation(summary = "Получение списка карточек команд")
  ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(
      @ParameterObject
      @PageableDefault
      Pageable pageable,
      @Valid @RequestBody FilterRequest filters);

  @GetMapping(value = "team-card",
              produces = "application/json")
  @Operation(summary = "Получение карточки команды")
  ResponseEntity<TeamCardDto> getTeamCard(
      @Parameter(description = "Идентификатор карточки команды",
                 example = "123e4567-e89b-12d3-a456-426614174000")
      @RequestParam UUID id,
      @RequestParam String username);

  @DeleteMapping(value = "team-card")
  @Operation(summary = "Удаление карточки команды")
  ResponseEntity<Void> deleteTeamCard(
      @Parameter(description = "Идентификатор карточки команды",
                 example = "123e4567-e89b-12d3-a456-426614174000")
      @RequestParam UUID id,
      @RequestParam String username);

  @GetMapping("team-cards/by-user")
  @Operation(summary = "Получить список команд пользователя")
  ResponseEntity<List<Map<String, String>>> getTeamsByUser(
      @Parameter(description = "Имя пользователя")
      @RequestParam String username);

  @PostMapping("team-cards/reassign")
  @Operation(summary = "Переназначить команды с одного пользователя на другого")
  ResponseEntity<Void> reassignTeams(
      @Valid @RequestBody ReassignTeamsRequest request);
}
