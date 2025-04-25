package net.akarmanov.projectplace.sso.config.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
import static net.akarmanov.projectplace.sso.config.security.SecurityConfiguration.LOGIN_PAGE;
import static net.akarmanov.projectplace.sso.config.security.SecurityConfiguration.PERMIT_ALL_PATTERNS;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class OAuth2AuthorizationServerConfig {

  private final AuthorizationServerProperties authorizationServerProperties;

  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
    return context -> {
      if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        return;
      }
      context.getClaims().claims(claims -> {
        var principal = context.getPrincipal();
        Set<String> roles =
            AuthorityUtils.authorityListToSet(principal.getAuthorities())
                .stream()
                .map(c -> c.replaceFirst("^ROLE_", ""))
                .collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
        claims.put("roles", roles);
        var authorization = context.getAuthorization();
        if (authorization != null) {
          claims.put("authorization_id", authorization.getId());
        }
      });
    };
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
    var authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

    var endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();
    http.securityMatcher(endpointsMatcher)
        .authorizeHttpRequests(authorize ->
            authorize
                // ендпоинты которые вынесем из под security
                .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
                .anyRequest().authenticated()
        )
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
            new LoginUrlAuthenticationEntryPoint(LOGIN_PAGE)
        ))
        .with(authorizationServerConfigurer, configurer ->
            configurer.oidc(Customizer.withDefaults())
        );
    return http.build();
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .issuer(authorizationServerProperties.getIssuerUrl())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
