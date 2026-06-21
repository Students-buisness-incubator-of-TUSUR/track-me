package net.trackme.sso.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Слушатель внутренних событий системы для синхронизации данных SSO.
 * Реагирует на успешную фиксацию транзакций и инициирует отправку уведомлений в Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SsoSyncEventListener {

    private final SsoEventsProducer ssoEventsProducer;

    /**
     * Обрабатывает событие обновления профиля пользователя после успешного коммита транзакции.
     *
     * @param event внутреннее событие обновления данных пользователя.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedInternalEvent event) {
        log.info(
            "[Sync] Транзакция зафиксирована. " +
            "Инициирована отправка события обновления для пользователя: {}",
            event.username()
        );

        ssoEventsProducer.sendUserUpdatedEvent(UserUpdatedEvent.builder()
                .username(event.username())
                .newFullName(event.newFullName())
                .build());
    }
}