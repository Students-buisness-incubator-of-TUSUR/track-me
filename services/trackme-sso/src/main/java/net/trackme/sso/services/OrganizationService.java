package net.trackme.sso.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dto.OrganizationCreateDto;
import net.trackme.sso.dto.OrganizationDto;
import net.trackme.sso.dto.OrganizationPublicDto;
import net.trackme.sso.dto.OrganizationUpdateDto;

/**
 * Сервис управления организациями.
 */
public interface OrganizationService {

  /**
   * Организация по умолчанию, к которой относятся данные, созданные до появления организаций.
   */
  UUID DEFAULT_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  /**
   * Создаёт новую организацию.
   *
   * @param createDto данные организации
   * @return созданная организация
   */
  OrganizationDto create(OrganizationCreateDto createDto);

  /**
   * Изменяет название и срок действия организации.
   *
   * @param id        идентификатор организации
   * @param updateDto новые данные
   * @return обновлённая организация
   */
  OrganizationDto update(UUID id, OrganizationUpdateDto updateDto);

  /**
   * Возвращает все организации в алфавитном порядке.
   *
   * @return список организаций
   */
  List<OrganizationDto> findAll();

  /**
   * Возвращает организацию по идентификатору.
   *
   * @param id идентификатор организации
   * @return организация
   */
  OrganizationDto get(UUID id);

  /**
   * Возвращает доступные организации для выбора при регистрации.
   *
   * @param query строка поиска по названию (может быть null)
   * @return список организаций в алфавитном порядке
   */
  List<OrganizationPublicDto> findAvailable(String query);

  /**
   * Временно блокирует организацию.
   *
   * @param id идентификатор организации
   */
  void block(UUID id);

  /**
   * Разблокирует организацию.
   *
   * @param id идентификатор организации
   */
  void unblock(UUID id);

  /**
   * Удаляет организацию и все связанные с ней данные.
   *
   * @param id           идентификатор организации
   * @param confirmation название организации для подтверждения удаления
   */
  void delete(UUID id, String confirmation);

  /**
   * Проверяет, доступна ли организация для регистрации и входа.
   *
   * @param id идентификатор организации
   * @return true, если организация активна и не истекла
   */
  boolean isAvailable(UUID id);

  /**
   * Возвращает доступную организацию или выбрасывает исключение.
   *
   * @param id идентификатор организации
   * @return организация
   */
  OrganizationEntity getAvailableOrganization(UUID id);

  /**
   * Помечает истёкшие организации как заблокированные.
   *
   * @param today текущая дата
   * @return количество заблокированных организаций
   */
  int blockExpired(LocalDate today);
}
