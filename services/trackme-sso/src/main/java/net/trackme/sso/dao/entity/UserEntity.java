package net.trackme.sso.dao.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.trackme.commons.dao.VersionedBusinessEntity;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "sso", name = "users")
public class UserEntity extends VersionedBusinessEntity<UUID> {
  @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
  @JoinTable(schema = "sso", name = "user_roles",
             joinColumns = @JoinColumn(name = "user_id"),
             inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  public Set<RoleEntity> roles = new HashSet<>();

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

  @ColumnDefault("false")
  @Column(name = "phone_number")
  private String phoneNumber;

  @ColumnDefault("true")
  @Column(name = "account_non_locked")
  private Boolean accountNonLocked = true;

}
