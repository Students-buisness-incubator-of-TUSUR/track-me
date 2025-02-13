package net.akarmanov.projectplace.configuration;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.services.acl.PostgresJdbcMutableAclService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;

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
    expressionHandler.setPermissionEvaluator(new AclPermissionEvaluator(mutableAclService()));
    expressionHandler.setRoleHierarchy(roleHierarchy);
    return expressionHandler;
  }

  @Bean
  public MutableAclService mutableAclService() {
    return new PostgresJdbcMutableAclService(dataSource, lookupStrategy(), aclCache(), properties);
  }

  @Bean
  public LookupStrategy lookupStrategy() {
    var lookupStrategy = new BasicLookupStrategy(
        dataSource,
        aclCache(),
        aclAuthorizationStrategy(),
        aclPermissionGrantingStrategy()
    );
    lookupStrategy.setAclClassIdSupported(true);
    return lookupStrategy;
  }

  @Bean
  public SpringCacheBasedAclCache aclCache() {
    var cache = new ConcurrentMapCache("aclCache");
    return new SpringCacheBasedAclCache(cache, aclPermissionGrantingStrategy(), aclAuthorizationStrategy());
  }

  @Bean
  public AclAuthorizationStrategy aclAuthorizationStrategy() {
    return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Bean
  public PermissionGrantingStrategy aclPermissionGrantingStrategy() {
    return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
  }
}
