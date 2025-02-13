package net.akarmanov.projectplace.rest.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Tag(name = "User Photo API",
     description = "Операции с фото пользователя")
@RequestMapping("/api/v1/users/{userId}/photo")
public interface UserPhotoRestController {

  @Operation(summary = "Добавить фото пользователя",
             parameters = {
                 @Parameter(name = "userId",
                            description = "Идентификатор пользователя",
                            required = true)
             })
  @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Void> addPhoto(@PathVariable UUID userId, @RequestParam MultipartFile file);

  @Operation(
      summary = "Получить фото пользователя",
      parameters = {
          @Parameter(name = "userId",
                     description = "Идентификатор пользователя",
                     required = true)
      })
  @GetMapping(produces = APPLICATION_OCTET_STREAM_VALUE)
  ResponseEntity<Resource> getPhoto(@PathVariable UUID userId);

  @Operation(
      summary = "Удалить фото пользователя",
      parameters = {
          @Parameter(name = "userId",
                     description = "Идентификатор пользователя",
                     required = true)
      })
  @DeleteMapping(produces = APPLICATION_OCTET_STREAM_VALUE)
  ResponseEntity<Void> deletePhoto(@PathVariable UUID userId);
}
