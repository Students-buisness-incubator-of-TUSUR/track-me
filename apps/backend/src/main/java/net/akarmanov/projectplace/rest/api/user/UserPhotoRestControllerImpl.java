package net.akarmanov.projectplace.rest.api.user;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.services.user.UserPhotoService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@RestController
@RequiredArgsConstructor
public class UserPhotoRestControllerImpl implements UserPhotoRestController {

  private final UserPhotoService userPhotoService;

  @Override
  public ResponseEntity<Void> addPhoto(String telegramId, MultipartFile file) {
    userPhotoService.addPhotoToUser(telegramId, file);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Resource> getPhoto(String telegramId) {
    var userPhoto = userPhotoService.getPhotoByTelegramId(telegramId);
    var photo = userPhoto.photo();
    return ResponseEntity.ok()
        .contentLength(photo.length)
        .header("Content-Disposition", "attachment; filename=" + userPhoto.fileName())
        .contentType(APPLICATION_OCTET_STREAM)
        .body(new ByteArrayResource(photo));
  }

  @Override
  public ResponseEntity<Void> deletePhoto(String telegramId) {
    userPhotoService.deletePhoto(telegramId);
    return ResponseEntity.ok().build();
  }
}
