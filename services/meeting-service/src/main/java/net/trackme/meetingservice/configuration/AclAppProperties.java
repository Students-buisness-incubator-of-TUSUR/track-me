package net.trackme.meetingservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки ACL.
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/authorization/acls.html">Spring Security ACL</a>
 */
@Data
@ConfigurationProperties(prefix = "app.acl")
public class AclAppProperties {
    /**
     * Запрос для получения идентификатора класса.
     */
    private final String classIdentityQuery;

    /**
     * Запрос для получения идентификатора объекта.
     */
    private final String sidIdentityQuery;
}

