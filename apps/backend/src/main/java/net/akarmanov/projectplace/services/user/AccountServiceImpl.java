package net.akarmanov.projectplace.services.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.mapping.UserMapper;
import net.akarmanov.projectplace.rest.api.dto.UserCreateDto;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.rest.api.dto.UserUpdateDTO;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final UserService userService;

  private final UserMapper userMapper;

  private final PasswordEncoder passwordEncoder;

  private final RestClient asRestClient;

  @Override
  public UserDTO getCurrentUserInfo() {
    var user = userService.getCurrentUser();
    return userMapper.mapUserToDto(user);
  }

  @Override
  public UserDTO updateUserInfo(UserUpdateDTO userDTO) {
    var user = userService.getCurrentUser();
    userMapper.updateFromDto(userDTO, user);
    var saved = userService.updateUser(user.getId(), user);
    return userMapper.mapUserToDto(saved);
  }

  @Override
  public void changePassword(String oldPassword, String newPassword) {
    userService.changePassword(oldPassword, newPassword);
  }

  @Override
  @Transactional
  public void registerUser(UserCreateDto userDto) {
    var user = userMapper.mapDtoToUser(userDto);
    user.setPassword(passwordEncoder.encode(userDto.getPassword()));
    var registerUserFeature = runAsync(() -> registerUserInAs(userDto))
        .exceptionally(ex -> {
          throw new UserRegistrationException();
        });
    userService.createUser(user);
    registerUserFeature.join();
  }

  private void registerUserInAs(UserCreateDto userCreateDto) {
    var requestBody = """
        {
          "username": "%s",
          "password": "%s",
          "roles": ["%s"]
        }
        """.formatted(
        userCreateDto.getTelegramId(),
        userCreateDto.getPassword(),
        String.join("\",\"", userCreateDto.getRole().name())
    );
    var response = asRestClient.put()
        .uri("/api/v1/users/update")
        .body(requestBody)
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatusCode::is5xxServerError, (rq, rs) -> {
          log.error("Error registering user in AS - 5xx: {}", rs.getStatusCode());
          throw new UserRegistrationException();
        })
        .onStatus(HttpStatusCode::is4xxClientError, (rq, rs) -> {
          log.error("Error registering user in AS - 4xx: {}", rs.getStatusCode());
          throw new UserRegistrationException();
        })
        .toBodilessEntity();
    if (!response.getStatusCode().is2xxSuccessful()) {
      log.error("Error registering user in AS: {}", response.getStatusCode());
      throw new UserRegistrationException();
    }
    log.info("User {} registered in AS", userCreateDto.getTelegramId());
  }
}
