package net.trackme.meetingservice.messaging.sso;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Слушатель событий SSO для синхронизации данных пользователей (трекеров).
 * Реагирует на обновления и обновляет соответствующие записи во встречах.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoEventConsumer {

    private final MeetingMetadataRepository metadataRepository;

    /**
     * Обрабатывает событие обновления профиля пользователя.
     *
     * @param event событие с данными обновленного пользователя.
     */
    @Transactional
    @KafkaListener(topics = "user-updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("[Kafka] Синхронизация данных: обновление ФИО трекера '{}' на '{}'",
                event.username(), event.newFullName());

        try {
            metadataRepository.updateTrackerFullNameByUsername(event.username(), event.newFullName());
            log.info("[Kafka] ФИО успешно обновлено для трекера '{}'", event.username());
        } catch (Exception e) {
            log.error("[Kafka] Ошибка при обновлении ФИО трекера '{}': {}", event.username(), e.getMessage());
            throw e;
        }
    }
}