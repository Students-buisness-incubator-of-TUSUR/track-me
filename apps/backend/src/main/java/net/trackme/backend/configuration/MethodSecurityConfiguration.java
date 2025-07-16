package net.trackme.backend.configuration;

import lombok.RequiredArgsConstructor;
import net.trackme.commons.acl.AclService;
import net.trackme.commons.acl.PostgresJdbcMutableAclService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;

/**
 * Настройки безопасности на уровне вызова методов.
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html">Spring Method Security</a>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(AclAppProperties.class)
public class MethodSecurityConfiguration {

    private final DataSource dataSource;

    private final AclAppProperties properties;

    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler(RoleHierarchy roleHierarchy) {
        var expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new AclPermissionEvaluator(mutableAclService(roleHierarchy)));
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public MutableAclService mutableAclService(RoleHierarchy roleHierarchy) {
        return new PostgresJdbcMutableAclService(dataSource,
                lookupStrategy(roleHierarchy),
                aclCache(roleHierarchy),
                properties.getClassIdentityQuery(),
                properties.getSidIdentityQuery());
    }

    @Bean
    AclService aclService(MutableAclService mutableAclService) {
        return new AclService(mutableAclService);
    }

    @Bean
    public LookupStrategy lookupStrategy(RoleHierarchy roleHierarchy) {
        var lookupStrategy = new BasicLookupStrategy(
                dataSource,
                aclCache(roleHierarchy),
                aclAuthorizationStrategy(roleHierarchy),
                aclPermissionGrantingStrategy()
        );
        lookupStrategy.setAclClassIdSupported(true);
        return lookupStrategy;
    }

    @Bean
    public SpringCacheBasedAclCache aclCache(RoleHierarchy roleHierarchy) {
        var cache = new ConcurrentMapCache("aclCache");
        return new SpringCacheBasedAclCache(cache,
                aclPermissionGrantingStrategy(),
                aclAuthorizationStrategy(roleHierarchy));
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy(RoleHierarchy roleHierarchy) {
        var authorizationStrategy = new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorizationStrategy.setRoleHierarchy(roleHierarchy);
        return authorizationStrategy;
    }

    @Bean
    public PermissionGrantingStrategy aclPermissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
    }
}
