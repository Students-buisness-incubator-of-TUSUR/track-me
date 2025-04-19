package net.akarmanov.projectplace.sso.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "registration-store")
public class RegistrationStoreProperties {
  @NotNull
  private String cookieName;

  @NotNull
  private String cookieDomain;

  @NotNull
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration cookieMaxAge;
}
