package net.akarmanov.projectplace.services.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.domain.UserPhoto;
import net.akarmanov.projectplace.repos.UserPhotoRepository;
import net.akarmanov.projectplace.repos.UserRepository;
import net.akarmanov.projectplace.services.exceptions.PhotoNotFoundException;
import net.akarmanov.projectplace.services.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
class UserPhotoServiceImpl implements UserPhotoService {

  private final UserPhotoRepository userPhotoRepository;

  private final UserRepository userRepository;

  @SneakyThrows
  @Override
  @Transactional
  public void addPhotoToUser(String telegramId, MultipartFile file) {

    var user = userRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new UserNotFoundException(telegramId));

    addPhoto(user, file);
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
  public byte[] getPhotoByTelegramId(String telegramId) {
    var userPhoto = userPhotoRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new PhotoNotFoundException(telegramId));
    return userPhoto.getPhoto();
  }

  @SneakyThrows
  private void addPhoto(User user, MultipartFile file) {
    var userPhoto = userPhotoRepository.findByTelegramId(user.getTelegramId())
        .orElseGet(() -> UserPhoto.builder()
            .user(user)
            .build());

    userPhoto.setPhoto(file.getBytes());
    userPhoto.setFileName(file.getOriginalFilename());
    userPhotoRepository.save(userPhoto);
  }
}
