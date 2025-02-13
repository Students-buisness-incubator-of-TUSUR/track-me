package net.akarmanov.projectplace.rest.api.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.rest.api.dto.UserUpdateDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Account API",
     description = "API для работы с аккаунтом пользователя")
@RequestMapping("/api/v1/users/")
public interface AccountRestController {

  @GetMapping("/current/info")
  @Operation(summary = "Получение информации о текущем пользователе")
  ResponseEntity<UserDTO> getCurrentUserInfo();

  @PostMapping(
      value = "/current/update",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Обновление информации о текущем пользователе")
  ResponseEntity<UserDTO> updateCurrentUserInfo(@Valid @RequestBody UserUpdateDTO userDto);

  @PostMapping("/current/changePassword")
  @Operation(summary = "Изменение пароля текущего пользователя")
  ResponseEntity<Void> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword);
}
