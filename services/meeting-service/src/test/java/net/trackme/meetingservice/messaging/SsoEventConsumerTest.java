package net.trackme.meetingservice.messaging;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.messaging.sso.SsoEventConsumer;
import net.trackme.meetingservice.messaging.sso.UserUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SsoEventConsumerTest {

    @Mock
    private MeetingMetadataRepository metadataRepository;

    @InjectMocks
    private SsoEventConsumer ssoEventConsumer;

    @Test
    void handleUserUpdated_success() {
        var event = new UserUpdatedEvent("user1", "Новое ФИО");

        ssoEventConsumer.handleUserUpdated(event);

        verify(metadataRepository, times(1))
                .updateTrackerFullNameByUsername("user1", "Новое ФИО");
    }

    @Test
    void handleUserUpdated_exception_rethrows() {
        var event = new UserUpdatedEvent("user1", "Новое ФИО");
        doThrow(new RuntimeException("DB Error"))
                .when(metadataRepository).updateTrackerFullNameByUsername(anyString(), anyString());

        assertThrows(RuntimeException.class, () -> ssoEventConsumer.handleUserUpdated(event));

        verify(metadataRepository, times(1)).updateTrackerFullNameByUsername(any(), any());
    }
}