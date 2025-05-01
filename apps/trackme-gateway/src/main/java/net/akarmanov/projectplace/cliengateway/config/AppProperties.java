package net.akarmanov.projectplace.cliengateway.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  @NotBlank
  private String afterLoginUrl;

  @NotBlank
  private String afterLogoutUri;

  private List<String> allowedOrigins = new ArrayList<>();
}
