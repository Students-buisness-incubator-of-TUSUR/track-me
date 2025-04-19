package net.akarmanov.projectplace.sso.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  @NestedConfigurationProperty
  private final CorsProperties cors = new CorsProperties();

  @NestedConfigurationProperty
  private final SwaggerProperties swagger = new SwaggerProperties();

  @NotBlank(message = "URL cannot be blank")
  private String apiUrl;

  private MailProperties mail = new MailProperties();

  @Data
  public static class CorsProperties {
    private List<CorsConfig> configs = new ArrayList<>();

    public record CorsConfig(
        @NotBlank(message = "Origin cannot be blank")
        String origin,
        @NotBlank(message = "Method cannot be blank")
        String method,
        @NotBlank(message = "Header cannot be blank")
        String header,
        boolean allowCredentials,
        long maxAge,
        boolean exposedHeaders,
        boolean allowOrigin,
        boolean allowMethod,
        boolean allowHeader,
        long maxAgeSeconds
    ) {
    }
  }

  @Data
  public static class SwaggerProperties {
    private AuthTypesConfig authTypes = new AuthTypesConfig();

    private AuthOauthConfig authOauth = new AuthOauthConfig();

    @Data
    public static class AuthTypesConfig {
      private Boolean authHeaderEnabled = Boolean.FALSE;

      private Boolean clientCredentialsEnabled = Boolean.FALSE;

      private Boolean authorizationCodeEnabled = Boolean.FALSE;
    }

    @Data
    public static class AuthOauthConfig {
      @NotBlank(message = "Token URL cannot be null")
      private String tokenUrl;

      @NotBlank(message = "Authorization URL cannot be null")
      private String authorizationUrl;

      @NotBlank(message = "User Info URL cannot be null")
      private String refreshUrl;
    }
  }

  @Data
  public static class MailProperties {
    @NotBlank(message = "Mail from cannot be blank")
    private String from;

    @NotBlank(message = "Mail subject cannot be blank")
    private String subject;
  }
}
