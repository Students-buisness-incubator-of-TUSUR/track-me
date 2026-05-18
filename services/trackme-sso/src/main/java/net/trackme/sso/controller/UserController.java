package net.trackme.sso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.dto.UserDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@Tag(name = "API для управления пользователями")
@RequestMapping("/api/v1/users")
public interface UserController {
  @PostMapping("/enable")
  @Operation(summary = "Включить пользователя")
  ResponseEntity<Void> enableUser(@RequestParam String username);

  @PostMapping("/disable")
  @Operation(summary = "Отключить пользователя")
  ResponseEntity<Void> disableUser(@RequestParam String username);

  @PostMapping("/unlock")
  @Operation(summary = "Разблокировать пользователя")
  ResponseEntity<Void> unlockUser(@RequestParam String username);

  @DeleteMapping
  @Operation(summary = "Полностью удалить пользователя из системы")
  ResponseEntity<Void> deleteUser(@RequestParam String username);

  @GetMapping("/{username}/teams")
  @Operation(summary = "Получить список команд пользователя")
  ResponseEntity<List<Map<String, String>>> getUserTeams(@PathVariable String username);

  @GetMapping(path = "/{username}/info", produces = "application/json")
  @Operation(summary = "Получить информацию о пользователе")
  ResponseEntity<UserDto> getUserInfo(@PathVariable String username);

  @PostMapping(path = "/trackers", produces = "application/json",
               consumes = "application/json")
  @Operation(summary = "Получить список трекеров")
  ResponseEntity<PagedModel<UserDto>> getTrackers(@RequestBody @Valid FilterRequest filterRequest,
                                                  @PageableDefault(sort = "username")
                                                  @ParameterObject Pageable pageable);

  @PostMapping(path = "/administrators", produces = "application/json",
               consumes = "application/json")
  @Operation(summary = "Получить список администраторов")
  ResponseEntity<PagedModel<UserDto>> getAdmins(@RequestBody @Valid FilterRequest filterRequest,
                                                @PageableDefault(sort = "username")
                                                @ParameterObject Pageable pageable);
}
