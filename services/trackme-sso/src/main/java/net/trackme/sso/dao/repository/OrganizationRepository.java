package net.trackme.sso.dao.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.type.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий организаций.
 */
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

  /**
   * Находит организацию по названию без учёта регистра.
   *
   * @param name название организации
   * @return найденная организация
   */
  Optional<OrganizationEntity> findByNameIgnoreCase(String name);

  /**
   * Проверяет существование организации с указанным названием.
   *
   * @param name название организации
   * @return true, если организация существует
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Возвращает все организации в алфавитном порядке.
   *
   * @return список организаций
   */
  List<OrganizationEntity> findAllByOrderByNameAsc();

  /**
   * Ищет организации по части названия в алфавитном порядке.
   *
   * @param name часть названия
   * @return список организаций
   */
  List<OrganizationEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

  /**
   * Находит организации с указанным статусом и истёкшей датой действия.
   *
   * @param status статус
   * @param date   дата, до которой истёк срок действия
   * @return список организаций
   */
  List<OrganizationEntity> findByStatusAndExpirationDateBefore(
      OrganizationStatus status,
      LocalDate date);
}
