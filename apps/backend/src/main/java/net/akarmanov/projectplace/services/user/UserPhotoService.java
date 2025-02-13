package net.akarmanov.projectplace.services.user;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserPhotoService {
  UserPhotoDto getPhoto(UUID photoId);

  void addPhotoToUser(UUID userId, MultipartFile file);

  void addPhoto(MultipartFile file);

  void deletePhoto(UUID userId);

  UserPhotoDto getPhotoByUserId(UUID userId);
}
