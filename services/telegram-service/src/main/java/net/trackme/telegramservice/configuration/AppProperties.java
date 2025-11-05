package net.trackme.telegramservice.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки приложения
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Настройки телеграм бота
     */
    private TelegramBotProperties telegramBotProperties = new TelegramBotProperties();

    /**
     * Настройки телеграм бота
     */
    @Data
    public static class TelegramBotProperties {
        /**
         * Имя бота
         */
        @NotBlank(message="Bot username cannot be blank")
        private String botUsername;

        /**
         * Токен бота
         */
        @NotBlank(message="Bot token cannot be blank")
        private String botToken;
    }
}
