package net.trackme.telegramservice.configuration;

import net.trackme.telegramservice.AbstractIntegrationTest;
import net.trackme.telegramservice.dao.ChatRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationBotTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationBot notificationBot;

    @Autowired
    private ChatRepository chatRepository;

    @Test
    void onUpdateReceived_success() {
        // Arrange
        Update update = createUpdate(1L, 1, "/start", "tester");

        // Act
        notificationBot.onUpdateReceived(update);

        // Assert
        var chat = chatRepository.findByUsername("tester");
        assertEquals(1L, chat.getChatId());
    }

    @Test
    void getToken_success() {
        // Act
        var token = notificationBot.getBotToken();
        var username = notificationBot.getBotUsername();

        // Assert
        assertEquals("test-token", token);
        assertEquals("test-username", username);
    }

    private Update createUpdate(Long chatId, Integer messageId, String text, String username) {
        Update update = new Update();
        update.setUpdateId(1);

        Message message = new Message();
        message.setMessageId(messageId);
        message.setText(text);

        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setUserName(username);
        message.setChat(chat);

        update.setMessage(message);
        return update;
    }
}