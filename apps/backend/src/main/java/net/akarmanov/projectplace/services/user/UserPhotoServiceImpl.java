package net.akarmanov.projectplace.services.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.domain.UserPhoto;
import net.akarmanov.projectplace.mapping.UserPhotoMapper;
import net.akarmanov.projectplace.repos.UserPhotoRepository;
import net.akarmanov.projectplace.repos.UserRepository;
import net.akarmanov.projectplace.services.exceptions.PhotoNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserPhotoServiceImpl implements UserPhotoService {

  private final UserPhotoRepository userPhotoRepository;

  private final UserRepository userRepository;

  private final UserPhotoMapper userPhotoMapper;

  @Override
  public UserPhotoDto getPhoto(UUID photoId) {
    var userPhoto = userPhotoRepository.findById(photoId)
        .orElseThrow(() -> new PhotoNotFoundException(photoId));
    return userPhotoMapper.toModel(userPhoto);
  }

  @SneakyThrows
  @Override
  @Transactional
  public void addPhotoToUser(UUID userId, MultipartFile file) {

    var user = userRepository.findById(userId);
    if (user.isEmpty()) {
      deletePhoto(userId);
    } else {
      addPhoto(user.get(), file);
    }
  }

  @Override
  public void addPhoto(MultipartFile file) {

  }

  @Override
  @Transactional
  public void deletePhoto(UUID userId) {
    var userPhoto = userPhotoRepository.findByUserId(userId)
        .orElseThrow(() -> new PhotoNotFoundException(userId));
    userPhotoRepository.delete(userPhoto);
  }

  @Override
  @Transactional
  public UserPhotoDto getPhotoByUserId(UUID userId) {
    var userPhoto = userPhotoRepository.findByUserId(userId)
        .orElseThrow(() -> new PhotoNotFoundException(userId));
    return userPhotoMapper.toModel(userPhoto);
  }

  @SneakyThrows
  private void addPhoto(User user, MultipartFile file) {
    try (var is = file.getInputStream()) {
      var photo = UserPhoto.builder()
          .user(user)
          .photo(is.readAllBytes())
          .fileName(file.getOriginalFilename())
          .build();
      userPhotoRepository.save(photo);
    }
  }
}
