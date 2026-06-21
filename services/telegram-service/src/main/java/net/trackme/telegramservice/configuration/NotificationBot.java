package net.trackme.telegramservice.configuration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.telegramservice.services.ChatService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@AllArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class NotificationBot extends TelegramLongPollingBot {
    /**
     * Настройки приложения.
     */
    private final AppProperties appProperties;

    /**
     * Сервис чатов.
     */
    private final ChatService chatService;

    @Override
    public String getBotUsername() {
        return appProperties.getTelegramBotProperties().getBotUsername();
    }

    @Override
    public String getBotToken() {
        return appProperties.getTelegramBotProperties().getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getChat().getUserName();
            Integer messageId = update.getMessage().getMessageId();

            deleteMessage(chatId, messageId);

            if (message.startsWith("/start")) {
                sendMessage(chatId, MessageTemplates.START_MESSAGE_TEMPLATE);
                chatService.createChat(chatId, username);
            }
        }
    }

    /**
     * Отправить сообщение.
     * @param chatId Идентификатор чата в Telegram
     * @param text Текст сообщения
     */
    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Bot could not send a message.");
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage message = new DeleteMessage();
        message.setChatId(chatId);
        message.setMessageId(messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Bot could not delete a message.");
        }
    }
}
