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

@Service
@RequiredArgsConstructor
class UserPhotoServiceImpl implements UserPhotoService {

  private final UserPhotoRepository userPhotoRepository;

  private final UserRepository userRepository;

  private final UserPhotoMapper userPhotoMapper;

  @SneakyThrows
  @Override
  @Transactional
  public void addPhotoToUser(String telegramId, MultipartFile file) {

    var user = userRepository.findByTelegramId(telegramId);
    if (user.isEmpty()) {
      deletePhoto(telegramId);
    } else {
      addPhoto(user.get(), file);
    }
  }

  @Override
  @Transactional
  public void deletePhoto(String telegramId) {
    var userPhoto = userPhotoRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new PhotoNotFoundException(telegramId));
    userPhotoRepository.delete(userPhoto);
  }

  @Override
  @Transactional
  public UserPhotoDto getPhotoByTelegramId(String telegramId) {
    var userPhoto = userPhotoRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new PhotoNotFoundException(telegramId));
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
