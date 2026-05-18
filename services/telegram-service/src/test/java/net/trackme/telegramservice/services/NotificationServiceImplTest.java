package net.trackme.telegramservice.services;

import net.trackme.telegramservice.AbstractIntegrationTest;
import net.trackme.telegramservice.configuration.MessageTemplates;
import net.trackme.telegramservice.configuration.NotificationBot;
import net.trackme.telegramservice.dao.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceImplTest extends AbstractIntegrationTest {

    private static final String TEST_TEAM = "test team";
    private static final String TEST_STREAM = "test stream";
    private static final String TEST_LINK = "test link";

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRepository chatRepository;

    @MockitoBean
    private NotificationBot notificationBot;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationBot, chatRepository);
    }

    @Test
    void sendMeetingNotHappenedMessage_success() {
        Long chatId = 1L;
        String username = "testuser";
        chatService.createChat(chatId, username);

        notificationService.sendMeetingNotHappenedMessage(
                username, TEST_TEAM, TEST_STREAM, TEST_LINK, "Петров Петр Петрович");

        String expectedMessage = MessageTemplates.MEETING_NOT_HAPPENED_MESSAGE_TEMPLATE
                .replace("{trackerFullName}", "Петров Петр")
                .replace("{teamCardName}", TEST_TEAM)
                .replace("{streamName}", TEST_STREAM)
                .replace("{meetingLink}", TEST_LINK);

        verify(notificationBot).sendMessage(chatId, expectedMessage);
    }

    @Test
    void sendMeetingNotHappenedMessage_chatNotFound_shouldLogWarn() {
        notificationService.sendMeetingNotHappenedMessage(
                "nonexistent", TEST_TEAM, TEST_STREAM, TEST_LINK, "Иванов Иван");

        verify(notificationBot, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void sendMeetingNotHappenedMessage_nullTrackerFullName_shouldUseDefault() {
        Long chatId = 2L;
        String username = "testuser2";
        chatService.createChat(chatId, username);

        notificationService.sendMeetingNotHappenedMessage(
                username, TEST_TEAM, TEST_STREAM, TEST_LINK, null);

        String expectedMessage = MessageTemplates.MEETING_NOT_HAPPENED_MESSAGE_TEMPLATE
                .replace("{trackerFullName}", "Не назначен")
                .replace("{teamCardName}", TEST_TEAM)
                .replace("{streamName}", TEST_STREAM)
                .replace("{meetingLink}", TEST_LINK);

        verify(notificationBot).sendMessage(chatId, expectedMessage);
    }

    @Test
    void sendMeetingNotHappenedMessage_shortFullName_shouldNotCrash() {
        Long chatId = 3L;
        String username = "testuser3";
        chatService.createChat(chatId, username);

        notificationService.sendMeetingNotHappenedMessage(
                username, TEST_TEAM, TEST_STREAM, TEST_LINK, "Иванов");

        String expectedMessage = MessageTemplates.MEETING_NOT_HAPPENED_MESSAGE_TEMPLATE
                .replace("{trackerFullName}", "Иванов")
                .replace("{teamCardName}", TEST_TEAM)
                .replace("{streamName}", TEST_STREAM)
                .replace("{meetingLink}", TEST_LINK);

        verify(notificationBot).sendMessage(chatId, expectedMessage);
    }
}
