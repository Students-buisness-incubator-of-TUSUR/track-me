package net.trackme.sso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import net.trackme.sso.dto.OrganizationCreateDto;
import net.trackme.sso.dto.OrganizationDto;
import net.trackme.sso.dto.OrganizationPublicDto;
import net.trackme.sso.dto.OrganizationUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * API управления организациями.
 */
@Validated
@Tag(name = "API управления организациями")
@RequestMapping("/api/v1/organizations")
public interface OrganizationController {

  /**
   * Возвращает список всех организаций.
   *
   * @return организации в алфавитном порядке
   */
  @GetMapping
  @Operation(summary = "Получить список организаций")
  ResponseEntity<List<OrganizationDto>> findAll();

  /**
   * Возвращает организацию по идентификатору.
   *
   * @param id идентификатор организации
   * @return организация
   */
  @GetMapping("/{id}")
  @Operation(summary = "Получить организацию")
  ResponseEntity<OrganizationDto> get(@PathVariable UUID id);

  /**
   * Возвращает доступные организации для выбора при регистрации.
   *
   * @param query строка поиска по названию
   * @return организации в алфавитном порядке
   */
  @GetMapping("/available")
  @Operation(summary = "Получить организации, доступные при регистрации")
  ResponseEntity<List<OrganizationPublicDto>> findAvailable(
      @RequestParam(required = false) String query);

  /**
   * Создаёт новую организацию.
   *
   * @param createDto данные организации
   * @return созданная организация
   */
  @PostMapping
  @Operation(summary = "Создать организацию")
  ResponseEntity<OrganizationDto> create(
      @RequestBody @Valid OrganizationCreateDto createDto);

  /**
   * Изменяет организацию.
   *
   * @param id        идентификатор организации
   * @param updateDto новые данные
   * @return обновлённая организация
   */
  @PutMapping("/{id}")
  @Operation(summary = "Изменить организацию")
  ResponseEntity<OrganizationDto> update(@PathVariable UUID id,
                                         @RequestBody @Valid OrganizationUpdateDto updateDto);

  /**
   * Блокирует организацию.
   *
   * @param id идентификатор организации
   * @return пустой ответ
   */
  @PostMapping("/{id}/block")
  @Operation(summary = "Заблокировать организацию")
  ResponseEntity<Void> block(@PathVariable UUID id);

  /**
   * Разблокирует организацию.
   *
   * @param id идентификатор организации
   * @return пустой ответ
   */
  @PostMapping("/{id}/unblock")
  @Operation(summary = "Разблокировать организацию")
  ResponseEntity<Void> unblock(@PathVariable UUID id);

  /**
   * Удаляет организацию и все её данные.
   *
   * @param id           идентификатор организации
   * @param confirmation название организации для подтверждения
   * @return пустой ответ
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Удалить организацию и все связанные данные")
  ResponseEntity<Void> delete(@PathVariable UUID id,
                              @RequestParam String confirmation);
}
