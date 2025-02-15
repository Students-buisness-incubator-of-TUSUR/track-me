package net.akarmanov.projectplace.rest.api.stream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Stream API",
     description = "API для работы с потоками")
@RequestMapping("/api/v1/streams")
public interface StreamRestController {

  Integer MAX_FILE_SIZE = 100 * 1024 * 1024; // 100 MB

  @Operation(summary = "Получить текущий поток",
             description = "Возвращает информацию о текущем потоке")
  @GetMapping("/current")
  ResponseEntity<StreamDto> getCurrentStream();

  @Validated
  @Operation(summary = "Получить список потоков",
             description = "Возвращает список потоков с поддержкой пагинации")
  @PostMapping(consumes = "application/json",
               produces = "application/json")
  PagedModel<StreamDto> getStreams(@RequestBody @Valid FilterRequest filterRequest,
                                   @PageableDefault @ParameterObject Pageable pageable);

  @Operation(summary = "Получить изображение потока",
             description = "Возвращает изображение потока по идентификатору")
  @GetMapping(value = "/{streamId}/image",
              produces = "image/png")
  ResponseEntity<Resource> getStreamImage(@PathVariable UUID streamId);

  @Operation(summary = "Добавить изображение",
             description = "Добавляет изображение к потоку")
  @PostMapping(value = "/{streamId}/image",
               consumes = "multipart/form-data")
  ResponseEntity<Void> addImage(@PathVariable UUID streamId, @RequestParam MultipartFile file);

  @Operation(summary = "Получить список рынков НТИ",
             description = "Возвращает список рынков НТИ")
  @GetMapping("/nti-markets")
  List<NTIMarketDto> getNTIMarkets();
}
