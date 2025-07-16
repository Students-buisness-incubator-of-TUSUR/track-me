package net.trackme.meetingservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки приложения.
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /**
     * Название приложения.
     */
    private String name;

    /**
     * URL-адрес приложения.
     */
    private String appUrl;

    /**
     * URL-адрес API приложения.
     */
    private String apiUrl;

    /**
     * Настройки почты.
     */
    private MailProperties mail = new MailProperties();

    /**
     * Настройки почты.
     */
    @Data
    public static class MailProperties {
        /**
         * Почтовый адрес отправителя.
         */
        private String from;
    }
}
