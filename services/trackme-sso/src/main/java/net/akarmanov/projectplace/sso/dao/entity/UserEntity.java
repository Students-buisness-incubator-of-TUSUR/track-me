package net.akarmanov.projectplace.sso.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.akarmanov.projectplace.commons.dao.VersionedBusinessEntity;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "sso", name = "users")
public class UserEntity extends VersionedBusinessEntity<UUID> {
  @Id
  @Column(name = "user_id", nullable = false)
  @UuidGenerator
  private UUID id;

  @Size(max = 100)
  @NotNull
  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Size(max = 100)
  @NotNull
  @Column(name = "username", nullable = false, length = 100)
  private String username;

  @Size(max = 500)
  @Column(name = "password_hash", length = 500)
  private String passwordHash;

  @Size(max = 255)
  @Column(name = "full_name")
  private String fullName;

  @Size(max = 255)
  @Column(name = "avatar_url")
  private String avatarUrl;

  @NotNull
  @ColumnDefault("false")
  @Column(name = "active", nullable = false)
  private Boolean active = false;

}
