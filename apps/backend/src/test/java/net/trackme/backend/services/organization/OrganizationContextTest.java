package net.trackme.backend.services.organization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Тесты контекста организации в backend.
 */
class OrganizationContextTest {

  private static final UUID ORGANIZATION_A =
      UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private static final UUID ORGANIZATION_B =
      UUID.fromString("00000000-0000-0000-0000-00000000000b");

  private final OrganizationContext context = new OrganizationContext();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsOrganizationFromJwtClaims() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThat(context.getOrganizationId()).contains(ORGANIZATION_A);
    assertThat(context.getOrganizationName()).contains("Организация А");
  }

  @Test
  void missingJwtMeansUnscopedAccess() {
    authenticateWithMockUser("ROLE_ADMIN");

    assertThat(context.getOrganizationId()).isEmpty();
    assertThat(context.isOverlord()).isFalse();
    assertThatCode(() -> context.checkOrganizationAccess(ORGANIZATION_B))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsEntityFromAnotherOrganization() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThatThrownBy(() -> context.checkOrganizationAccess(ORGANIZATION_B))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void allowsEntityOfSameOrganization() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThatCode(() -> context.checkOrganizationAccess(ORGANIZATION_A))
        .doesNotThrowAnyException();
  }

  @Test
  void organizationlessLegacyEntityIsAvailable() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThatCode(() -> context.checkOrganizationAccess(null))
        .doesNotThrowAnyException();
  }

  @Test
  void overlordCanAccessAnyOrganization() {
    authenticate(null, null, "ROLE_OVERLORD");

    assertThat(context.isOverlord()).isTrue();
    assertThatCode(() -> context.checkOrganizationAccess(ORGANIZATION_B))
        .doesNotThrowAnyException();
  }

  private void authenticate(UUID organizationId, String organizationName, String... roles) {
    var builder = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user");
    if (organizationId != null) {
      builder.claim(OrganizationContext.ORGANIZATION_ID_CLAIM, organizationId.toString());
    }
    if (organizationName != null) {
      builder.claim(OrganizationContext.ORGANIZATION_NAME_CLAIM, organizationName);
    }
    var authorities = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new JwtAuthenticationToken(builder.build(), authorities));
  }

  private void authenticateWithMockUser(String... roles) {
    var authorities = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "user", "password", authorities));
  }
}
