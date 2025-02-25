package net.akarmanov.projectplace.rest.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "Stream API",
     description = "API для работы с потоками")
@RequestMapping(value = "/api/v1/admin/")
public interface StreamAdminRestController {

  @Operation(summary = "Создать новый поток",
             description = "Создает новый поток с заданными параметрами")
  @PostMapping(value = "/stream",
               consumes = "application/json",
               produces = "application/json")
  ResponseEntity<StreamDto> create(@Valid @RequestBody StreamCreateDto stream);

  @Operation(summary = "Обновить существующий поток",
             description = "Обновляет поток по заданному идентификатору")
  @PutMapping(value = "/stream/{streamId}",
              consumes = "application/json",
              produces = "application/json")
  ResponseEntity<StreamDto> update(
      @Parameter(description = "Идентификатор потока",
                 required = true) @PathVariable UUID streamId,
      @Valid @RequestBody StreamUpdateDto stream);

  @Operation(summary = "Удалить поток",
             description = "Удаляет поток по заданному идентификатору")
  @DeleteMapping(value = "/stream/{streamId}")
  ResponseEntity<Void> delete(
      @Parameter(description = "Идентификатор потока",
                 required = true) @PathVariable UUID streamId);

  @Operation(summary = "Получить все потоки",
             description = "Возвращает список всех потоков с поддержкой пагинации")
  @PostMapping(value = "/streams", produces = MediaType.APPLICATION_JSON_VALUE)
  PagedModel<StreamDto> findAll(@RequestBody @Valid FilterRequest filterRequest,
                                @PageableDefault(sort = "name") @ParameterObject Pageable pageable);

  @Operation(
      summary = "Получить поток по идентификатору",
      description = "Возвращает поток по заданному идентификатору")
  @GetMapping("/stream/{streamId}")
  ResponseEntity<StreamDto> getById(
      @Parameter(description = "Идентификатор потока",
                 required = true) @PathVariable("streamId") UUID streamId);
}
