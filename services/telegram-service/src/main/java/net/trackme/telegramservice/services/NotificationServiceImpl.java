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
                                              String meetingLink,
                                              String trackerFullName) {
        ChatEntity chat = chatRepository.findByUsername(teamCardUsername);

        if (chat == null) {
            log.warn("Chat with user {} not found", teamCardUsername);
            return;
        }

        String shortName = getShortName(trackerFullName);

        var message = MessageTemplates.MEETING_NOT_HAPPENED_MESSAGE_TEMPLATE
                .replace("{trackerFullName}", shortName)
                .replace("{teamCardName}", teamCardName)
                .replace("{streamName}", streamName)
                .replace("{meetingLink}", meetingLink);

        notificationBot.sendMessage(chat.getChatId(), message);
    }

    private String getShortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Не назначен";
        }
        String[] parts = fullName.trim().split(" ");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return fullName;
    }
}
