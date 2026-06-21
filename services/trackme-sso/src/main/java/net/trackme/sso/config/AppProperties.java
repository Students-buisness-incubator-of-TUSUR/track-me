package net.trackme.sso.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  @NestedConfigurationProperty
  private final SwaggerProperties swagger = new SwaggerProperties();

  @NotBlank(message = "URL cannot be blank")
  private String apiUrl;

  @NotBlank(message = "Telegram URL cannot be blank")
  private String telegramUrl;

  @NotBlank(message = "Bot username cannot be blank")
  private String botUsername;

  private MailProperties mail = new MailProperties();

  @NestedConfigurationProperty
  private ServicesProperties services = new ServicesProperties();

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

    private List<String> summarySendRoles;
  }

  @Data
  public static class ServicesProperties {
    private BackendProperties backend = new BackendProperties();

    @Data
    public static class BackendProperties {
      private String url;
    }
  }
}
