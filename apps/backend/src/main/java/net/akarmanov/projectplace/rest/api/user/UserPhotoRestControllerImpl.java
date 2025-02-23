package net.akarmanov.projectplace.rest.api.user;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.services.user.UserPhotoService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UserPhotoRestControllerImpl implements UserPhotoRestController {

  public static final int MAX_FILE_SIZE = 100 * 1024 * 1024;

  private final UserPhotoService userPhotoService;

  @Override
  public ResponseEntity<Void> addPhoto(String telegramId, MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File is too large");
    }

    if (!isPng(file)) {
      throw new IllegalArgumentException("File is not a PNG image");
    }

    userPhotoService.addPhotoToUser(telegramId, file);
    return ResponseEntity.ok().build();
  }

  private boolean isPng(MultipartFile file) {
    return MediaType.IMAGE_PNG_VALUE.equals(file.getContentType());
  }

  @Override
  public ResponseEntity<Resource> getPhoto(String telegramId) {
    var photo = userPhotoService.getPhotoByTelegramId(telegramId);
    return ResponseEntity.ok()
        .contentLength(photo.length)
        .contentType(MediaType.IMAGE_PNG)
        .body(new ByteArrayResource(photo));
  }

  @Override
  public ResponseEntity<Void> deletePhoto(String telegramId) {
    userPhotoService.deletePhoto(telegramId);
    return ResponseEntity.ok().build();
  }
}
