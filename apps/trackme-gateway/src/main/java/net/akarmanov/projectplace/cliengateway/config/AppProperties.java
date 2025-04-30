package net.akarmanov.projectplace.cliengateway.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  @NotBlank
  private String afterLoginUrl;

  @NotBlank
  private String afterLogoutUri;
}
