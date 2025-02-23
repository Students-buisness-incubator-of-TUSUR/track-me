package net.akarmanov.projectplace.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.akarmanov.projectplace.models.UserRole;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pp_user")
public class User implements UserDetails {

  @Id
  @Column(nullable = false,
          updatable = false)
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @NotNull
  @Size(max = 255)
  @Column(name = "password",
          nullable = false,
          unique = true)
  private String password;

  @Size(max = 255)
  @Column(name = "first_name")
  @Deprecated(forRemoval = true)
  private String firstName;

  @Size(max = 255)
  @Column(name = "last_name")
  @Deprecated(forRemoval = true)
  private String lastName;

  @Size(max = 255)
  @Column(name = "middle_name")
  @Deprecated(forRemoval = true)
  private String middleName;

  @Size(max = 255)
  @Column(name = "full_name")
  private String fullName;

  @Size(max = 32)
  @Column(name = "phone_number",
          length = 32,
          unique = true)
  private String phoneNumber;

  @Size(max = 32)
  @Column(name = "telegram_id",
          length = 32,
          nullable = false,
          unique = true)
  private String telegramId;

  @NotNull
  @Column(name = "role",
          nullable = false,
          length = 32)
  @Enumerated(EnumType.STRING)
  private UserRole role;

  @Column(name = "enabled",
          nullable = false)
  private boolean enabled;

  @Email
  private String email;

  @OneToMany(mappedBy = "user",
             cascade = CascadeType.ALL)
  @Builder.Default
  private Set<TeamCard> userTeamCards = new HashSet<>();

  @ManyToMany
  @JoinTable(
      name = "stream_user",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "stream_id")
  )
  @Builder.Default
  private Set<Stream> streams = new HashSet<>();

  @OneToOne(mappedBy = "user",
            cascade = CascadeType.ALL)
  private UserPhoto photo;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String getUsername() {
    return telegramId;
  }
}