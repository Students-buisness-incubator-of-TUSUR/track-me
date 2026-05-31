package net.trackme.sso.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Свойства приложения, загружаемые из конфигурации с префиксом "app".
 * Содержит настройки Swagger, почты и внешних сервисов.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Настройки Swagger для документирования API. */
    @NestedConfigurationProperty
    private final SwaggerProperties swagger = new SwaggerProperties();

    /** Базовый URL API-сервиса. */
    @NotBlank(message = "URL cannot be blank")
    private String apiUrl;

    /** URL для взаимодействия с Telegram. */
    @NotBlank(message = "Telegram URL cannot be blank")
    private String telegramUrl;

    /** Имя пользователя бота в Telegram. */
    @NotBlank(message = "Bot username cannot be blank")
    private String botUsername;

    /** Настройки для отправки почтовых сообщений. */
    private MailProperties mail = new MailProperties();

    /** Настройки для подключения к внешним сервисам. */
    @NestedConfigurationProperty
    private ServicesProperties services = new ServicesProperties();

    /**
     * Свойства конфигурации Swagger/OpenAPI.
     */
    @Data
    public static class SwaggerProperties {
        /** Настройки типов аутентификации. */
        private AuthTypesConfig authTypes = new AuthTypesConfig();

        /** Настройки OAuth-аутентификации. */
        private AuthOauthConfig authOauth = new AuthOauthConfig();

        /**
         * Конфигурация доступных типов аутентификации в Swagger.
         */
        @Data
        public static class AuthTypesConfig {
            /** Включение аутентификации через заголовок. */
            private Boolean authHeaderEnabled = Boolean.FALSE;

            /** Включение аутентификации через Client Credentials. */
            private Boolean clientCredentialsEnabled = Boolean.FALSE;

            /** Включение аутентификации через Authorization Code. */
            private Boolean authorizationCodeEnabled = Boolean.FALSE;
        }

        /**
         * Конфигурация OAuth-провайдера для Swagger.
         */
        @Data
        public static class AuthOauthConfig {
            /** URL для получения токена доступа. */
            @NotBlank(message = "Token URL cannot be null")
            private String tokenUrl;

            /** URL для авторизации пользователя. */
            @NotBlank(message = "Authorization URL cannot be null")
            private String authorizationUrl;

            /** URL для обновления токена. */
            @NotBlank(message = "User Info URL cannot be null")
            private String refreshUrl;
        }
    }

    /**
     * Свойства для настройки отправки электронной почты.
     */
    @Data
    public static class MailProperties {
        /** Адрес отправителя письма. */
        @NotBlank(message = "Mail from cannot be blank")
        private String from;

        /** Тема письма. */
        @NotBlank(message = "Mail subject cannot be blank")
        private String subject;

        /** Список ролей, которым отправляется сводка. */
        private List<String> summarySendRoles;
    }

    /**
     * Свойства для настройки подключения к внешним сервисам.
     */
    @Data
    public static class ServicesProperties {
        /** Настройки подключения к бэкенд-сервису. */
        private BackendProperties backend = new BackendProperties();

        /**
         * Свойства бэкенд-сервиса.
         */
        @Data
        public static class BackendProperties {
            /** URL бэкенд-сервиса. */
            private String url;
        }
    }
}
