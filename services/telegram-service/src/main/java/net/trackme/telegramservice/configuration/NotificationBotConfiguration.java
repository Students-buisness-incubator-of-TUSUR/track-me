package net.trackme.telegramservice.configuration;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
@AllArgsConstructor
public class NotificationBotConfiguration {
    /**
     * Бот уведомлений.
     */
    private final NotificationBot notificationBot;

    @PostConstruct
    public void registerNotificationBot() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(notificationBot);
            log.info("Telegram bot API registered notification bot.");
        } catch (TelegramApiException e) {
            log.error("Telegram bot API could not register notification bot.");
        }
    }
}
