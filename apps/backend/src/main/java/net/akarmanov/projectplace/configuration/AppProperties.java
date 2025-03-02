package net.akarmanov.projectplace.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private String name;
  private String appUrl;

  private MailProperties mail = new MailProperties();

  @Data
  public static class MailProperties {
    private String from;
  }
}
