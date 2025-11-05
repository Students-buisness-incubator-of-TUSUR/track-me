package net.trackme.telegramservice.configuration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final AppProperties appProperties;

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
        if(update.hasMessage() && update.getMessage().hasText()){
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            Integer messageId = update.getMessage().getMessageId();

            deleteMessage(chatId, messageId);
            handleMessage(chatId, message);
        }
    }

    public void handleMessage(Long chatId, String message){
        if (message.startsWith("/start")){
            String reply = """
                           Вас приветсвует бот оповещений TrackMe.
                           Я буду присылать вам сообщения о рейтинге вашей команды.
                           """;
            sendMessage(chatId, reply);
        }
    }

    public void sendMessage(Long chatId, String text){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Bot could not send a message.");
        }
    }

    private void deleteMessage(Long chatId, Integer messageId){
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
