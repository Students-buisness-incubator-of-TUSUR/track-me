package net.akarmanov.projectplace.sso.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver")
public class AuthorizationServerProperties {
  private String issuerUrl;

  private String introspectionEndpoint;

  private String authenticationSuccessUrl;

  private String customHandlerHeaderName;

  private long authorizationTtl;

  private long authorizationConsentTtl;
}
