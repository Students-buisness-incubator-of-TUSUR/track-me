package net.trackme.sso.services.impl;

import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.exception.EmailNotFoundException;
import net.trackme.sso.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultUserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void resetPassword_emailNotFound() {
        // Arrange
        var email = "john@john.john";
        var password = "testPassword@123";

        // Act && Assert
        assertThrows(EmailNotFoundException.class, () -> userService.resetPassword(email, password));
    }
}