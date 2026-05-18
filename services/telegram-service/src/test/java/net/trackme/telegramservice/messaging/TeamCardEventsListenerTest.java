package net.trackme.telegramservice.messaging;

import net.trackme.telegramservice.AbstractIntegrationTest;
import net.trackme.telegramservice.services.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class TeamCardEventsListenerTest extends AbstractIntegrationTest {

    private ConsumerRecord<String, MeetingNotHappenedEvent> consumerRecord;

    @Autowired
    private TeamCardEventsListener teamCardEventsListener;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void onMeetingNotHappenedEvent() {
        MeetingNotHappenedEvent event = new MeetingNotHappenedEvent(
                "test username",
                "test team card",
                "test stream",
                "test link",
                "Петров Петр Петрович"
        );

        consumerRecord = new ConsumerRecord<>("meeting-not-happened", 0, 0L, "key", event);

        teamCardEventsListener.onMeetingNotHappenedEvent(consumerRecord);

        verify(notificationService).sendMeetingNotHappenedMessage(
                "test username",
                "test team card",
                "test stream",
                "test link",
                "Петров Петр Петрович");
    }
}
