package net.akarmanov.projectplace.services.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.repos.UserRepository;
import net.akarmanov.projectplace.services.exceptions.NoMatchesPasswordException;
import net.akarmanov.projectplace.services.exceptions.PhoneNumberExistsException;
import net.akarmanov.projectplace.services.exceptions.TelegramIdExistsException;
import net.akarmanov.projectplace.services.exceptions.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  @Override
  public User getUser(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
  }

  @Override
  public User getUserByTelegramId(String telegramId) {
    return userRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new UserNotFoundException(telegramId));
  }

  @Override
  public User createUser(User userCreate) {
    if (userRepository.existsUserByTelegramId(userCreate.getTelegramId())) {
      throw new TelegramIdExistsException(userCreate.getTelegramId());
    }
    if (userRepository.existsByPhoneNumber(userCreate.getPhoneNumber())) {
      throw new PhoneNumberExistsException(userCreate.getPhoneNumber());
    }
    return userRepository.save(userCreate);
  }

  @Override
  @Transactional
  public User updateUser(UUID id, User userDTO) {
    var user = userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
    user.setTelegramId(userDTO.getTelegramId());
    user.setFullName(userDTO.getFullName());
    user.setRole(userDTO.getRole());
    user.setPhoneNumber(userDTO.getPhoneNumber());
    return userRepository.save(user);
  }

  @Override
  public void deleteUser(String id) {
    var user = getUser(UUID.fromString(id));
    userRepository.delete(user);
  }

  @Override
  public User getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return getUserByTelegramId(auth.getName());
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.existsUserByTelegramId(username);
  }

  @Override
  public Page<User> findAll(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  @Override
  public void enableUser(UUID userId) {
    changeUserState(userId, true);
  }

  @Override
  public void disableUser(UUID userId) {
    changeUserState(userId, false);
  }

  @Override
  public void changePassword(String oldPassword, String newPassword) {
    var user = getCurrentUser();
    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
      throw new NoMatchesPasswordException();
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  private void changeUserState(UUID userId, boolean enabled) {
    var user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
    user.setEnabled(enabled);
    userRepository.save(user);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return getUserByTelegramId(username);
  }
}
