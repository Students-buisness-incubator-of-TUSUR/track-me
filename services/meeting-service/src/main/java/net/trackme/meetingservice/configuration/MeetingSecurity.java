package net.trackme.meetingservice.configuration;

import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.dao.MeetingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Хелпер для SpEL-проверок в @PreAuthorize.
 * Используется, когда ACL встречи не покрывает случай
 * "трекер, которому передана команда".
 *
 * <p>Проверяет денормализованное поле {@code Meeting.trackerUsername},
 * которое обновляется через Kafka-событие {@code team-card-updated}
 * (см. {@code TeamCardEventConsumer.handleTeamCardUpdated} →
 * {@code MeetingMetadataRepository.updateMetadata}).
 */
@Component("meetingSecurity")
@RequiredArgsConstructor
public class MeetingSecurity {

    private final MeetingRepository meetingRepository;

    /**
     * true, если текущий пользователь — трекер, которому принадлежит
     * команда встречи (по денормализованному полю Meeting.trackerUsername).
     *
     * @param meetingId      идентификатор встречи
     * @param authentication текущая аутентификация
     * @return true, если пользователь является трекером встречи
     */
    public boolean isTrackerOfMeeting(UUID meetingId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return false;
        }
        return meetingRepository.findById(meetingId)
                .map(m -> username.equalsIgnoreCase(m.getTrackerUsername()))
                .orElse(false);
    }
}
