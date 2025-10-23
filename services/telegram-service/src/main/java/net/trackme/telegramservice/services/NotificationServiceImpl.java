package net.trackme.telegramservice.services;

import lombok.RequiredArgsConstructor;
import net.trackme.telegramservice.configuration.NotificationBot;
import net.trackme.telegramservice.dao.ChatRepository;
import net.trackme.telegramservice.entities.ChatEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationBot notificationBot;

    private final ChatRepository chatRepository;

    @Override
    public void sendMeetingNotHappenedMessage(String username,
                                              String meetingLink,
                                              BigDecimal averageGrade) {
        ChatEntity chat = chatRepository.findByUsername(username);

        if (chat == null)
            return;

        var message = String.format("""
                      Запланированная вами встреча не состоялась.
                      Вы можете узнать информацию о пропущенной встрече, перейдя по ссылке:
                      %s
                      """, meetingLink);

        if (averageGrade.compareTo(BigDecimal.valueOf(0.25)) < 0) {
            message += """
                       Рейтинг вашей команды опустился ниже 0,25.
                       Посещение запланированных встреч повысит ваш рейтинг.
                       """;
        }
        notificationBot.sendMessage(chat.getChatId(), message);
    }
}
