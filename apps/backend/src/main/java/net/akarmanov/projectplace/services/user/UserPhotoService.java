package net.akarmanov.projectplace.services.user;

import org.springframework.web.multipart.MultipartFile;

public interface UserPhotoService {

  void addPhotoToUser(String telegramId, MultipartFile file);

  void deletePhoto(String telegramId);

  byte[] getPhotoByTelegramId(String telegramId);
}
