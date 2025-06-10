package net.trackme.sso.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

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
  private Duration cookieMaxAge;
}
