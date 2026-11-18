package net.trackme.sso.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.trackme.commons.dao.VersionedBusinessEntity;
import net.trackme.sso.type.OrganizationStatus;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

/**
 * Организация (тенант), объединяющая пользователей и их данные.
 */
@Getter
@Setter
@Entity
@Table(schema = "sso", name = "organizations")
public class OrganizationEntity extends VersionedBusinessEntity<UUID> {

  @Id
  @Column(name = "organization_id", nullable = false)
  @UuidGenerator
  private UUID id;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @ColumnDefault("'ACTIVE'")
  @Column(name = "status", nullable = false, length = 32)
  private OrganizationStatus status = OrganizationStatus.ACTIVE;

  @Column(name = "expiration_date")
  private LocalDate expirationDate;

  /**
   * Проверяет, истёк ли срок действия организации.
   *
   * @param today текущая дата
   * @return true, если срок действия истёк
   */
  public boolean isExpired(LocalDate today) {
    return expirationDate != null && expirationDate.isBefore(today);
  }

  /**
   * Проверяет, доступна ли организация для входа пользователей.
   *
   * @param today текущая дата
   * @return true, если организация активна и не истекла
   */
  public boolean isAvailable(LocalDate today) {
    return status == OrganizationStatus.ACTIVE && !isExpired(today);
  }
}
