package net.trackme.commons.acl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AclServiceTest {

    @Mock
    private MutableAclService mutableAclService;

    @Mock
    private MutableAcl mutableAcl;

    @Mock
    private MutableAcl parentAcl;

    private AclService aclService;

    public static class TestIdentity {
        private final String id;

        public TestIdentity(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @BeforeEach
    void setUp() {
        aclService = new AclService(mutableAclService);
    }

    @Test
    void createAclForUser_ShouldCreateAclWithOwnerPermissions() {
        // Given
        String ownerUsername = "testUser";
        var identity = new TestIdentity("testObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);

        // When
        aclService.createAclForUser(identity, ownerUsername);

        // Then
        ArgumentCaptor<ObjectIdentity> objectIdentityCaptor = ArgumentCaptor.forClass(
                ObjectIdentity.class);
        verify(mutableAclService).createAcl(objectIdentityCaptor.capture());

        ObjectIdentity capturedIdentity = objectIdentityCaptor.getValue();
        assertEquals(identity.getClass().getName(), capturedIdentity.getType());
        assertEquals(identity.getId(), capturedIdentity.getIdentifier().toString());

        // Verify owner is set
        verify(mutableAcl).setOwner(any(PrincipalSid.class));

        // Verify full permissions are granted to owner
        verify(mutableAcl, times(12)).insertAce(
                anyInt(), any(Permission.class), any(Sid.class),
                eq(true));

        // Verify admin roles get full permissions
        verify(mutableAcl, times(8)).insertAce(
                anyInt(), any(Permission.class), any(GrantedAuthoritySid.class), eq(true));

        verify(mutableAclService, times(4)).updateAcl(mutableAcl);
    }

    @Test
    void createAclForUserWithCreator_ShouldCreateAclWithOwnerAndCreatorPermissions() {
        // Given
        String ownerUsername = "owner";
        String creatorUsername = "creator";
        var identity = new TestIdentity("testObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);

        // When
        aclService.createAclForUser(identity, ownerUsername, creatorUsername);

        // Then
        verify(mutableAclService).createAcl(any(ObjectIdentity.class));
        verify(mutableAcl).setOwner(any(PrincipalSid.class));

        // Verify both owner and creator get permissions
        verify(mutableAcl, times(8)).insertAce(
                anyInt(), any(Permission.class), any(PrincipalSid.class), eq(true));

        // Verify admin roles get permissions
        verify(mutableAcl, times(8)).insertAce(
                anyInt(), any(Permission.class), any(GrantedAuthoritySid.class), eq(true));

        verify(mutableAclService, times(5)).updateAcl(mutableAcl);
    }

    @Test
    void createAclForUserWithParent_ShouldCreateAclWithParentInheritance() {
        // Given
        String ownerUsername = "owner";
        String creatorUsername = "creator";
        Object identity = new TestIdentity("childObject");
        Object parent = new TestIdentity("parentObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);
        when(mutableAclService.readAclById(any(ObjectIdentity.class))).thenReturn(parentAcl);

        // When
        aclService.createAclForUserWithParent(identity, ownerUsername, creatorUsername, parent);

        // Then
        verify(mutableAclService).createAcl(any(ObjectIdentity.class));
        verify(mutableAclService).readAclById(any(ObjectIdentity.class));
        verify(mutableAcl).setParent(parentAcl);
        verify(mutableAcl).setOwner(any(PrincipalSid.class));
        verify(mutableAclService, times(5)).updateAcl(mutableAcl);
    }

    @Test
    void createAclForUserWithParent_WhenParentNotFound_ShouldHandleException() {
        // Given
        String ownerUsername = "owner";
        String creatorUsername = "creator";
        var identity = new TestIdentity("childObject");
        var parent = new TestIdentity("parentObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);
        when(mutableAclService.readAclById(any(ObjectIdentity.class)))
                .thenThrow(new NotFoundException("Parent ACL not found"));

        assertThrows(
                NotFoundException.class, () -> {
                    aclService.createAclForUserWithParent(
                            identity, ownerUsername, creatorUsername, parent);
                });

        verify(mutableAclService).createAcl(any(ObjectIdentity.class));
        verify(mutableAclService).readAclById(any(ObjectIdentity.class));
        verify(mutableAcl, never()).setParent(any());
    }

    @Test
    void createAclForUser_WithNullIdentity_ShouldThrowException() {
        // Given
        String ownerUsername = "testUser";
        Object identity = null;

        // When & Then
        assertThrows(
                IllegalArgumentException.class, () -> {
                    aclService.createAclForUser(identity, ownerUsername);
                });

        verify(mutableAclService, never()).createAcl(any());
    }

    @Test
    void createAclForUser_WithNullOwner_ShouldThrowException() {
        // Given
        String ownerUsername = null;
        Object identity = new TestIdentity("testObject");

        // When & Then
        assertThrows(
                IllegalArgumentException.class, () -> {
                    aclService.createAclForUser(identity, ownerUsername);
                });

        verify(mutableAclService).createAcl(any());
    }

    @Test
    void createAclForUser_WithEmptyOwner_ShouldThrowException() {
        // Given
        String ownerUsername = "";
        var identity = new TestIdentity("testObject");

        // When & Then
        assertThrows(
                IllegalArgumentException.class, () -> {
                    aclService.createAclForUser(identity, ownerUsername);
                });

        verify(mutableAclService).createAcl(any());
    }

    @Test
    void createAclForUser_WhenAclServiceThrowsException_ShouldPropagateException() {
        // Given
        String ownerUsername = "testUser";
        Object identity = new TestIdentity("testObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(
                RuntimeException.class, () -> {
                    aclService.createAclForUser(identity, ownerUsername);
                });
    }

    @Test
    void createAclForUser_ShouldSetCorrectOwner() {
        // Given
        String ownerUsername = "testOwner";
        Object identity = new TestIdentity("testObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);

        // When
        aclService.createAclForUser(identity, ownerUsername);

        // Then
        ArgumentCaptor<PrincipalSid> ownerCaptor = ArgumentCaptor.forClass(PrincipalSid.class);
        verify(mutableAcl).setOwner(ownerCaptor.capture());

        PrincipalSid capturedOwner = ownerCaptor.getValue();
        assertEquals(ownerUsername, capturedOwner.getPrincipal());
    }

    @Test
    void createAclForUser_ShouldGrantCorrectPermissions() {
        // Given
        String ownerUsername = "testUser";
        Object identity = new TestIdentity("testObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);

        // When
        aclService.createAclForUser(identity, ownerUsername);

        // Then
        ArgumentCaptor<Permission> permissionCaptor = ArgumentCaptor.forClass(Permission.class);
        verify(mutableAcl, atLeast(4)).insertAce(
                anyInt(), permissionCaptor.capture(), any(Sid.class), eq(true));

        var capturedPermissions = permissionCaptor.getAllValues();
        assertTrue(capturedPermissions.contains(BasePermission.READ));
        assertTrue(capturedPermissions.contains(BasePermission.WRITE));
        assertTrue(capturedPermissions.contains(BasePermission.DELETE));
        assertTrue(capturedPermissions.contains(BasePermission.ADMINISTRATION));
    }

    @Test
    void createAclForUser_ShouldGrantPermissionsToAdminRoles() {
        // Given
        String ownerUsername = "testUser";
        var identity = new TestIdentity("testObject");

        when(mutableAclService.createAcl(any(ObjectIdentity.class))).thenReturn(mutableAcl);

        // When
        aclService.createAclForUser(identity, ownerUsername);

        // Then
        ArgumentCaptor<GrantedAuthoritySid> authorityCaptor = ArgumentCaptor.forClass(
                GrantedAuthoritySid.class);
        verify(mutableAcl, atLeast(8)).insertAce(
                anyInt(), any(Permission.class), authorityCaptor.capture(), eq(true));

        var capturedAuthorities = authorityCaptor.getAllValues();
        assertTrue(capturedAuthorities.stream().anyMatch(
                sid -> "ROLE_SUPER_ADMIN".equals(sid.getGrantedAuthority())));
        assertTrue(capturedAuthorities.stream().anyMatch(
                sid -> "ROLE_ADMIN".equals(sid.getGrantedAuthority())));
    }
}