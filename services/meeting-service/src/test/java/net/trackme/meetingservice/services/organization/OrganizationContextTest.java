package net.trackme.meetingservice.services.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Тесты контекста организации в meeting-сервисе.
 */
class OrganizationContextTest {

  private static final UUID ORGANIZATION_A =
      UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private static final UUID ORGANIZATION_B =
      UUID.fromString("00000000-0000-0000-0000-00000000000b");

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void readsOrganizationFromJwtClaims() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThat(OrganizationContext.getOrganizationId()).contains(ORGANIZATION_A);
    assertThat(OrganizationContext.getOrganizationName()).contains("Организация А");
  }

  @Test
  void missingJwtMeansUnscopedAccess() {
    authenticateWithMockUser("ROLE_ADMIN");

    assertThat(OrganizationContext.getOrganizationId()).isEmpty();
    assertThat(OrganizationContext.isOverlord()).isFalse();
    assertThatCode(() -> OrganizationContext.checkOrganizationAccess(ORGANIZATION_B))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsMeetingFromAnotherOrganization() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThatThrownBy(() -> OrganizationContext.checkOrganizationAccess(ORGANIZATION_B))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void allowsMeetingOfSameOrganization() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThatCode(() -> OrganizationContext.checkOrganizationAccess(ORGANIZATION_A))
        .doesNotThrowAnyException();
  }

  @Test
  void legacyMeetingWithoutOrganizationIsAvailable() {
    authenticate(ORGANIZATION_A, "Организация А", "ROLE_TRACKER");

    assertThatCode(() -> OrganizationContext.checkOrganizationAccess(null))
        .doesNotThrowAnyException();
  }

  @Test
  void overlordCanAccessAnyOrganization() {
    authenticate(null, null, "ROLE_OVERLORD");

    assertThat(OrganizationContext.isOverlord()).isTrue();
    assertThatCode(() -> OrganizationContext.checkOrganizationAccess(ORGANIZATION_B))
        .doesNotThrowAnyException();
  }

  @Test
  void overlordAuthorityWithoutRolePrefixIsRecognized() {
    authenticateClaims(null, null, "OVERLORD");

    assertThat(OrganizationContext.isOverlord()).isTrue();
  }

  @Test
  void noAuthenticationMeansNotOverlordAndUnscopedAccess() {
    SecurityContextHolder.clearContext();

    assertThat(OrganizationContext.isOverlord()).isFalse();
    assertThat(OrganizationContext.getOrganizationId()).isEmpty();
    assertThatCode(() -> OrganizationContext.checkOrganizationAccess(ORGANIZATION_B))
        .doesNotThrowAnyException();
  }

  @Test
  void jwtWithoutOrganizationClaimsMeansUnscopedAccess() {
    authenticate(null, null, "ROLE_TRACKER");

    assertThat(OrganizationContext.getOrganizationId()).isEmpty();
    assertThat(OrganizationContext.getOrganizationName()).isEmpty();
    assertThatCode(() -> OrganizationContext.checkOrganizationAccess(ORGANIZATION_B))
        .doesNotThrowAnyException();
  }

  @Test
  void blankOrganizationClaimsAreIgnored() {
    authenticateClaims("", "", "ROLE_TRACKER");

    assertThat(OrganizationContext.getOrganizationId()).isEmpty();
    assertThat(OrganizationContext.getOrganizationName()).isEmpty();
  }

  @Test
  void invalidOrganizationIdClaimIsIgnored() {
    authenticateClaims("not-a-uuid", "Организация А", "ROLE_TRACKER");

    assertThat(OrganizationContext.getOrganizationId()).isEmpty();
    assertThat(OrganizationContext.getOrganizationName()).contains("Организация А");
  }

  private static void authenticate(UUID organizationId, String organizationName,
                                   String... roles) {
    authenticateClaims(organizationId != null ? organizationId.toString() : null,
        organizationName, roles);
  }

  private static void authenticateClaims(String organizationId, String organizationName,
                                         String... roles) {
    var builder = Jwt.withTokenValue("token").header("alg", "none").subject("user");
    if (organizationId != null) {
      builder.claim(OrganizationContext.ORGANIZATION_ID_CLAIM, organizationId);
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

  private static void authenticateWithMockUser(String... roles) {
    var authorities = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "user", "password", authorities));
  }
}
