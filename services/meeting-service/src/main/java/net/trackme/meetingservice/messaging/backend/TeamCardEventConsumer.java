package net.trackme.meetingservice.messaging.backend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCardEventConsumer {

    private final MeetingMetadataRepository metadataRepository;
    private final SsoApiClient ssoApiClient;

    /**
     * Обрабатывает изменение названия команды или смену трекера.
     */
    @Transactional
    @KafkaListener(topics = "team-card-updated")
    public void handleTeamCardUpdated(TeamCardUpdatedEvent event) {
        log.info("[Kafka] Обновление метаданных для команды {}. Новое название: {}, новый трекер: {}",
                event.teamCardId(), event.newName(), event.newUsername());

        String newUsername = event.newUsername();

        String fullName = null;
        String trackerId = null;

        if (newUsername != null) {
            try {
                log.debug("[Kafka] Получен новый username: '{}', поиск в SSO...", event.newUsername());
                var trackerData = ssoApiClient.getTrackers().stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(event.newUsername()))
                        .findFirst();

                if (trackerData.isPresent()) {
                    fullName = trackerData.get().getFullName();
                    trackerId = trackerData.get().getId();
                    log.debug("[Kafka] Трекер найден в SSO: '{}'. Установлены fullName: '{}' и trackerId: '{}'",
                            event.newUsername(), fullName, trackerId);
                } else {
                    log.warn("[Kafka] Трекер {} не найден в SSO для синхронизации", event.newUsername());
                }
            } catch (Exception e) {
                log.error("[Kafka] Ошибка при получении данных из SSO во время синхронизации: {}", e.getMessage());
            }
        } else {
            log.debug("[Kafka] Username трекера не изменился или отсутствует, обновление данных SSO не требуется.");
        }

        metadataRepository.updateMetadata(
                event.teamCardId(),
                event.newName(),
                event.newUsername(),
                trackerId,
                fullName
        );
    }

    /**
     * Обрабатывает добавление привязки команды к потоку.
     */
    @Transactional
    @KafkaListener(topics = "team-card-stream-added")
    public void handleStreamAdded(TeamCardStreamAddedEvent event) {
        log.debug("[Kafka] Добавление стрима {} команде {}", event.streamId(), event.teamCardId());
        metadataRepository.addStreamToTeamMeetings(event.teamCardId(), event.streamId());
    }

    /**
     * Обрабатывает удаление привязки команды к потоку.
     */
    @Transactional
    @KafkaListener(topics = "team-card-stream-removed")
    public void handleStreamRemoved(TeamCardStreamRemovedEvent event) {
        log.debug("[Kafka] Удаление стрима {} у команды {}", event.streamId(), event.teamCardId());
        metadataRepository.removeStreamFromTeamMeetings(event.teamCardId(), event.streamId());
    }
}