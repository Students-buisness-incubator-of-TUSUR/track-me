package net.trackme.sso.dto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizedUserSerializationTest {
    @Test
    void readsUserSerializedBeforeProviderAttributesBecameTransient() throws Exception {
        try (var resource = getClass().getResourceAsStream("/legacy-authorized-user.ser");
             var input = new ObjectInputStream(resource)) {
            var restored = (AuthorizedUser) input.readObject();
            assertEquals("legacy-tracker", restored.getUsername());
            assertEquals("Legacy Tracker", restored.getFullName());
            assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), restored.getId());
            assertTrue(restored.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_TRACKER".equals(authority.getAuthority())));
            assertNull(restored.getAttributes());
        }
    }

    @Test
    void sessionRetainsIdentityAndAuthoritiesWithoutPersistingProviderAttributes() throws Exception {
        UUID id = UUID.randomUUID();
        var user = AuthorizedUser.builder("tracker", "password", true, true, true, true,
                        List.of(new SimpleGrantedAuthority("ROLE_TRACKER")))
                .id(id).fullName("Tracker Name").phoneNumber("+79001234567")
                .oauthAttributes(Map.of("providerOnly", new Object())).build();
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(user);
        }
        AuthorizedUser restored;
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (AuthorizedUser) input.readObject();
        }
        assertEquals(id, restored.getId());
        assertEquals(user.getUsername(), restored.getUsername());
        assertEquals(user.getFullName(), restored.getFullName());
        assertEquals(user.getPhoneNumber(), restored.getPhoneNumber());
        assertEquals(user.getAuthorities(), restored.getAuthorities());
        assertTrue(restored.isEnabled());
        assertTrue(restored.isAccountNonLocked());
        assertNull(restored.getAttributes());
    }
}
