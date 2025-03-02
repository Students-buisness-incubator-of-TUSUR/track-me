package net.akarmanov.projectplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.HOURS;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "password_reset_token")
public class PasswordResetToken {
  private static final int EXPIRATION_HOURS = 1;

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  private String token;

  @ManyToOne
  @JoinColumn(nullable = false, name = "user_id")
  private User user;

  @Column(nullable = false, name = "expiration_date")
  private Instant expiryDate;

  public PasswordResetToken(String token, User user) {
    this.token = token;
    this.user = user;
    this.expiryDate = Instant.now().plus(EXPIRATION_HOURS, HOURS);
  }
}
