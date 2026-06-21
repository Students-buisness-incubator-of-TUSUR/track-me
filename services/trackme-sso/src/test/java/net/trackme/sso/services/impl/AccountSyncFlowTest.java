package net.trackme.sso.services.impl;

import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dto.UserUpdateDto;
import net.trackme.sso.messaging.SsoEventsProducer;
import net.trackme.sso.services.AccountService;
import net.trackme.sso.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccountSyncFlowTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @MockitoBean
    private UserService userService;

    @MockitoSpyBean
    private SsoEventsProducer ssoEventsProducer;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void updateUser_shouldUpdateDbAndSendToKafka() {
        //  Arrange
        String username = "test_user";
        UserUpdateDto updateDto = new UserUpdateDto(
                "Ivan Ivanov",
                "http://avatar.png",
                null,
                null
        );

        UserEntity existingUser = new UserEntity();
        existingUser.setUsername(username);
        existingUser.setFullName("Old Name");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(existingUser);

        // Act
        accountService.updateUser(updateDto, auth);

        // Assert: БД
        verify(userService).save(argThat(user ->
                user.getFullName().equals("Ivan Ivanov")
        ));
        assertEquals("Ivan Ivanov", existingUser.getFullName());

        // Assert: Проверка работы TransactionalEventListener
        verify(ssoEventsProducer, timeout(2000)).sendUserUpdatedEvent(argThat(event ->
                event.username().equals(username) &&
                        event.newFullName().equals("Ivan Ivanov")
        ));

        // Assert: Проверка финальной отправки в Kafka
        verify(kafkaTemplate).send(any(Message.class));
    }
}