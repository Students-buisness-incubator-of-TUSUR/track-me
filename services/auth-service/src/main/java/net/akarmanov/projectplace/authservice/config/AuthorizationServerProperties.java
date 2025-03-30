package net.akarmanov.projectplace.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.authorizationserver")
public class AuthorizationServerProperties {
  private JdbcProperties jdbc = new JdbcProperties();

  public JdbcProperties getJdbc() {
    return jdbc;
  }

  public static class JdbcProperties {
    private String updateUserSql;

    private String selectUserSql;

    public String getUpdateUserSql() {
      return updateUserSql;
    }

    public void setUpdateUserSql(String updateUserSql) {
      this.updateUserSql = updateUserSql;
    }

    public String getSelectUserSql() {
      return selectUserSql;
    }

    public void setSelectUserSql(String selectUserSql) {
      this.selectUserSql = selectUserSql;
    }
  }
}
