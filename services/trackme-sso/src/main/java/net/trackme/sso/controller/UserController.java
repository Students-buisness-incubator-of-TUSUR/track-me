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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления пользователями системы.
 * Предоставляет API для включения, отключения, удаления пользователей
 * и получения информации о них.
 */
@Validated
@Tag(name = "API для управления пользователями")
@RequestMapping("/api/v1/users")
public interface UserController {

    /**
     * Включает учётную запись пользователя.
     *
     * @param username имя пользователя
     * @return ответ без содержимого
     */
    @PostMapping("/enable")
    @Operation(summary = "Включить пользователя")
    ResponseEntity<Void> enableUser(@RequestParam String username);

    /**
     * Отключает учётную запись пользователя.
     *
     * @param username имя пользователя
     * @return ответ без содержимого
     */
    @PostMapping("/disable")
    @Operation(summary = "Отключить пользователя")
    ResponseEntity<Void> disableUser(@RequestParam String username);

    /**
     * Разблокирует учётную запись пользователя.
     *
     * @param username имя пользователя
     * @return ответ без содержимого
     */
    @PostMapping("/unlock")
    @Operation(summary = "Разблокировать пользователя")
    ResponseEntity<Void> unlockUser(@RequestParam String username);

    /**
     * Полностью удаляет пользователя из системы.
     *
     * @param username имя пользователя
     * @return ответ без содержимого
     */
    @DeleteMapping
    @Operation(summary = "Полностью удалить пользователя из системы")
    ResponseEntity<Void> deleteUser(@RequestParam String username);

    /**
     * Получает список команд, в которых состоит пользователь.
     *
     * @param username имя пользователя
     * @return список команд пользователя
     */
    @GetMapping("/{username}/teams")
    @Operation(summary = "Получить список команд пользователя")
    ResponseEntity<List<Map<String, String>>> getUserTeams(
            @PathVariable String username);

    /**
     * Получает подробную информацию о пользователе.
     *
     * @param username имя пользователя
     * @return информация о пользователе
     */
    @GetMapping(path = "/{username}/info", produces = "application/json")
    @Operation(summary = "Получить информацию о пользователе")
    ResponseEntity<UserDto> getUserInfo(@PathVariable String username);

    /**
     * Получает список трекеров с фильтрацией и пагинацией.
     *
     * @param filterRequest параметры фильтрации
     * @param pageable параметры пагинации
     * @return страница с трекерами
     */
    @PostMapping(path = "/trackers", produces = "application/json",
                 consumes = "application/json")
    @Operation(summary = "Получить список трекеров")
    ResponseEntity<PagedModel<UserDto>> getTrackers(
            @RequestBody @Valid FilterRequest filterRequest,
            @PageableDefault(sort = "username")
            @ParameterObject Pageable pageable);

    /**
     * Получает список администраторов с фильтрацией и пагинацией.
     *
     * @param filterRequest параметры фильтрации
     * @param pageable параметры пагинации
     * @return страница с администраторами
     */
    @PostMapping(path = "/administrators", produces = "application/json",
                 consumes = "application/json")
    @Operation(summary = "Получить список администраторов")
    ResponseEntity<PagedModel<UserDto>> getAdmins(
            @RequestBody @Valid FilterRequest filterRequest,
            @PageableDefault(sort = "username")
            @ParameterObject Pageable pageable);
}
