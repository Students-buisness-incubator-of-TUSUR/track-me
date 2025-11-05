package net.trackme.telegramservice.configuration;

import net.trackme.telegramservice.AbstractIntegrationTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationBotTest extends AbstractIntegrationTest {

    @Test
    void onUpdateReceived_success() throws Exception {
        // Arrange
        String testText = """
                          Вас приветсвует бот оповещений TrackMe.
                          Я буду присылать вам сообщения о рейтинге вашей команды.
                          """;
        Update update = createUpdate(1L, 1, "/start");
        AppProperties appProperties = createAppProperties("test-token", "test-username");
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(1L);
        sendMessage.setText(testText);

        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(1L);
        deleteMessage.setMessageId(1);

        NotificationBot notificationBot = Mockito.spy(new NotificationBot(appProperties));

        // Act
        notificationBot.onUpdateReceived(update);

        // Assert
        verify(notificationBot).execute(sendMessage);
        verify(notificationBot).execute(deleteMessage);
    }

    @Test
    void getToken_success() {
        // Arrange
        AppProperties appProperties = createAppProperties("test-token", "test-username");
        NotificationBot notificationBot = new NotificationBot(appProperties);

        // Act
        var token = notificationBot.getBotToken();
        var username = notificationBot.getBotUsername();

        // Assert
        assertEquals("test-token", token);
        assertEquals("test-username", username);
    }

    private Update createUpdate(Long chatId, Integer messageId, String text) {
        Update update = new Update();
        update.setUpdateId(1);

        Message message = new Message();
        message.setMessageId(messageId);
        message.setText(text);

        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);

        update.setMessage(message);
        return update;
    }

    private AppProperties createAppProperties(String botToken, String botUsername){
        AppProperties.TelegramBotProperties telegramBotProperties = new AppProperties.TelegramBotProperties();
        telegramBotProperties.setBotToken(botToken);
        telegramBotProperties.setBotUsername(botUsername);

        AppProperties appProperties = new AppProperties();
        appProperties.setTelegramBotProperties(telegramBotProperties);

        return appProperties;
    }
}