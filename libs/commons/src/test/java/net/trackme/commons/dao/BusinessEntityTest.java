package net.trackme.commons.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessEntityTest {

    private BusinessEntity<Long> businessEntity;

    @BeforeEach
    void setUp() {
        businessEntity = new BusinessEntity<>() {
            @Override
            public Long getId() {
                return null;
            }

            @Override
            public void setId(Long aLong) {
                // No implementation needed for this test
            }
        };
    }

    @Test
    void shouldSetDefaultValuesOnPrePersistWhenCreatedByIsNull() {
        SecurityContextHolder.clearContext();

        businessEntity.prePersist();

        assertNotNull(businessEntity.getCreationDate());
        assertNotNull(businessEntity.getLastUpdateDate());
        assertEquals(BusinessEntity.DEFAULT_USER, businessEntity.getCreatedBy());
        assertEquals(BusinessEntity.DEFAULT_USER, businessEntity.getLastUpdatedBy());
    }

    @Test
    void shouldSetCustomCreatedByOnPrePersist() {
        businessEntity.setCreatedBy("testUser");

        businessEntity.prePersist();

        assertEquals("testUser", businessEntity.getCreatedBy());
        assertEquals("testUser", businessEntity.getLastUpdatedBy());
    }

    @Test
    void shouldSetDefaultLastUpdatedByOnPrePersist() {
        var mockSecurityContext = mock(SecurityContext.class);
        var mockAuthentication = mock(Authentication.class);
        var mockUserDetails = mock(UserDetails.class);

        when(mockUserDetails.getUsername()).thenReturn("currentUser");
        when(mockAuthentication.getPrincipal()).thenReturn(mockUserDetails);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockSecurityContext);

        businessEntity.prePersist();

        assertEquals("currentUser", businessEntity.getCreatedBy());
        assertEquals("currentUser", businessEntity.getLastUpdatedBy());

        businessEntity.preUpdate();

        assertNotNull(businessEntity.getLastUpdateDate());
    }
}