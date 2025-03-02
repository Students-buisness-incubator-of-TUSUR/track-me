package net.akarmanov.projectplace.services;

import net.akarmanov.projectplace.AbstractIntegrationTest;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.repos.UserRepository;
import net.akarmanov.projectplace.services.exceptions.UserNotFoundException;
import net.akarmanov.projectplace.services.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "JWT_SECRET=12345678905675675674564564566756756756745645656"
    })
class UserServiceImplIntegrationTest extends AbstractIntegrationTest {
  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  private User user;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    user = new User();
    user.setEmail("john@example.com");
    user.setPhoneNumber("+71234567890");
    user.setTelegramId("telegramId");
    user.setRole(UserRole.ADMIN);
    user.setPassword("password");
    user = userRepository.save(user);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void testGetUser_success() {
    User userDTO = userService.getUser(user.getId());
    assertNotNull(userDTO);
    assertEquals(user.getPhoneNumber(), userDTO.getPhoneNumber());
    assertEquals(user.getTelegramId(), userDTO.getTelegramId());
    assertEquals(user.getRole().toString(), userDTO.getRole().toString());
  }

  @Test
  void testGetUser_notFound() {
    var uuid = UUID.randomUUID();
    assertThrows(UserNotFoundException.class, () -> userService.getUser(uuid));
  }

  @Test
  void testCreateUser() {
    var userServiceUser = User.builder()
        .email("john4@example.com")
        .phoneNumber("+79876543210")
        .telegramId("newTelegramId")
        .password("newPassword")
        .role(UserRole.ADMIN)
        .build();

    User createdUserDTO = userService.createUser(userServiceUser);
    assertNotNull(createdUserDTO);
    assertEquals(userServiceUser.getPhoneNumber(), createdUserDTO.getPhoneNumber());
    assertEquals(userServiceUser.getTelegramId(), createdUserDTO.getTelegramId());

    Optional<User> createdUser = userRepository.findById(createdUserDTO.getId());
    assertTrue(createdUser.isPresent());
    assertEquals(userServiceUser.getPhoneNumber(), createdUser.get().getPhoneNumber());
    assertEquals(userServiceUser.getTelegramId(), createdUser.get().getTelegramId());
    assertEquals(userServiceUser.getRole().toString(), createdUser.get().getRole().toString());
  }

  @Test
  void testUpdateUser() {
    User updateUser = User.builder()
        .fullName("John Smith Middle")
        .email("john@example.com")
        .phoneNumber("+79876543210")
        .telegramId("newTelegramId")
        .password("newPassword")
        .role(UserRole.ADMIN)
        .build();

    var updatedUserDTO = userService.updateUser(user.getId(), updateUser);
    assertNotNull(updatedUserDTO);
    assertEquals(updateUser.getFullName(), updatedUserDTO.getFullName());
    assertEquals(updateUser.getPhoneNumber(), updatedUserDTO.getPhoneNumber());
    assertEquals(updateUser.getTelegramId(), updatedUserDTO.getTelegramId());

    Optional<User> updatedUser = userRepository.findById(user.getId());
    assertTrue(updatedUser.isPresent());
    assertEquals(updateUser.getFullName(), updatedUser.get().getFullName());
    assertEquals(updateUser.getPhoneNumber(), updatedUser.get().getPhoneNumber());
    assertEquals(updateUser.getTelegramId(), updatedUser.get().getTelegramId());
    assertEquals(updateUser.getRole().toString(), updatedUser.get().getRole().toString());
  }
}