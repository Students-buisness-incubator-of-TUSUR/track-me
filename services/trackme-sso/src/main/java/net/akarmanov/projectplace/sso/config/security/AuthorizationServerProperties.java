package net.akarmanov.projectplace.sso.config.security;

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
@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver")
public class AuthorizationServerProperties {
  private String issuerUrl;

  private String registrationConfirmationEndpoint;

  private String authenticationSuccessUrl;

  private String customHandlerHeaderName;

  @DurationUnit(ChronoUnit.MILLIS)
  private Duration authorizationTtl;

  @DurationUnit(ChronoUnit.MILLIS)
  private Duration authorizationConsentTtl;
}
