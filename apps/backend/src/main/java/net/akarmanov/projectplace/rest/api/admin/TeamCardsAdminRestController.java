package net.akarmanov.projectplace.rest.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Team Cards API",
     description = "API для работы с карточками команд для администратора")
@RequestMapping("/api/v1/admin/team-cards")
public interface TeamCardsAdminRestController {
  @PostMapping(value = "/create",
               consumes = "application/json",
               produces = "application/json")
  @Operation(summary = "Создание карточки команды")
  ResponseEntity<TeamCardDto> createTeamCard(@Valid @RequestBody TeamCardCreateOrUpdateDto teamCardDto,
                                             @RequestParam UUID userId);

  @PostMapping(value = "/update",
               consumes = "application/json",
               produces = "application/json")
  @Operation(summary = "Обновление карточки команды")
  ResponseEntity<TeamCardDto> updateTeamCard(@RequestParam UUID teamCardId,
                                             @Parameter @RequestParam UUID userId,
                                             @Valid @RequestBody TeamCardCreateOrUpdateDto teamCardDto);

  @PostMapping(produces = "application/json")
  @Operation(summary = "Получение списка карточек команд")
  ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(
      @Parameter(description = "Название карточки команды")
      @RequestParam(required = false)
      String name,
      @Parameter(description = "Статус карточки команды")
      @RequestParam(required = false)
      String status,
      @ParameterObject
      @PageableDefault
      Pageable pageable);

  @GetMapping(value = "/get",
              produces = "application/json")
  @Operation(summary = "Получение карточки команды")
  ResponseEntity<TeamCardDto> getTeamCard(
      @Parameter(description = "Идентификатор карточки команды",
                 example = "123e4567-e89b-12d3-a456-426614174000")
      @RequestParam
      UUID id,
      @RequestParam UUID userId);

  @DeleteMapping(value = "/delete")
  @Operation(summary = "Удаление карточки команды")
  ResponseEntity<Void> deleteTeamCard(
      @Parameter(description = "Идентификатор карточки команды",
                 example = "123e4567-e89b-12d3-a456-426614174000")
      @RequestParam
      UUID id,
      @RequestParam UUID userId);
}
