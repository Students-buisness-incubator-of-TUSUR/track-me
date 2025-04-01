package net.akarmanov.projectplace.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties("app.authorizationserver")
public class AuthorizationServerProperties {
  private final JdbcProperties jdbc = new JdbcProperties();

  @Setter
  @Getter
  public static class JdbcProperties {
    private String updateUserSql;

    private String selectUserSql;

    private String roleSql;

    private String authoritiesByUsernameSql;
  }
}
