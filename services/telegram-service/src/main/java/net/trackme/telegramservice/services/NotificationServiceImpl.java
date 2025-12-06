package net.trackme.telegramservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.telegramservice.configuration.MessageTemplates;
import net.trackme.telegramservice.configuration.NotificationBot;
import net.trackme.telegramservice.dao.ChatRepository;
import net.trackme.telegramservice.entities.ChatEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    /**
     * Бот уведомлений.
     */
    private final NotificationBot notificationBot;

    /**
     * Репозиторий чатов.
     */
    private final ChatRepository chatRepository;

    @Override
    public void sendMeetingNotHappenedMessage(String teamCardUsername,
                                              String teamCardName,
                                              String streamName,
                                              String meetingLink) {
        ChatEntity chat = chatRepository.findByUsername(teamCardUsername);

        if (chat == null) {
            log.warn("Chat with user {} not found", teamCardUsername);
            return;
        }

        var message = MessageTemplates.MEETING_NOT_HAPPENED_MESSAGE_TEMPLATE
                .replace("{teamCardName}", teamCardName)
                .replace("{streamName}", streamName)
                .replace("{meetingLink}", meetingLink);

        notificationBot.sendMessage(chat.getChatId(), message);
    }
}
