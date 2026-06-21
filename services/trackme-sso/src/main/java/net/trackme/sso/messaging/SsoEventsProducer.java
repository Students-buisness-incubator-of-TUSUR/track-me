package net.trackme.sso.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Продюсер событий SSO-сервиса.
 * Отвечает за публикацию изменений данных пользователей в брокер сообщений Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoEventsProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_UPDATED_TOPIC = "user-updated";

    /**
     * Отправляет событие об обновлении профиля пользователя.
     *
     * @param event данные с обновленным именем пользователя и его логином.
     */
    public void sendUserUpdatedEvent(UserUpdatedEvent event) {
        log.info("[Kafka] Отправка события обновления пользователя '{}' в топик '{}'",
                event.username(), USER_UPDATED_TOPIC);

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, USER_UPDATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.username())
                .build();
        kafkaTemplate.send(message);
    }
}