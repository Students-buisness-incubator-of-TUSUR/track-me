package net.akarmanov.projectplace.sso.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.akarmanov.projectplace.commons.dao.VersionedBusinessEntity;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "sso", name = "roles")
public class RoleEntity extends VersionedBusinessEntity<UUID> {
  @Id
  @Column(name = "role_id", nullable = false)
  @UuidGenerator
  private UUID id;

  @Column(name = "role_code", nullable = false)
  private String code;

  @Column(name = "role_name", nullable = false)
  private String name;

  @Column(name = "active")
  private Boolean active;
}
