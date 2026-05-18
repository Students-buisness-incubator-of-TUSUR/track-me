package net.trackme.backend.services.teamcard;

import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Сервис для управления данными карточек команд на основе встреч.
 */
public interface TeamCardMeetingsService {

    /**
     * Увеличивает счетчик встреч для карточки команды.
     *
     * @param teamCardId идентификатор карточки команды
     * @param meetingId идентификатор встречи
     */
    void increaseMeetingCount(UUID teamCardId, UUID meetingId);

    /**
     * Обрабатывает удаление встречи.
     *
     * @param teamCardId идентификатор карточки команды
     * @param meetingId идентификатор встречи
     */
    void handleMeetingDeleted(UUID teamCardId, UUID meetingId);

    /**
     * Обновляет информацию карточки команды на основе данных встречи.
     *
     * @param teamCardId идентификатор карточки команды
     * @param uuid идентификатор встречи
     * @param newStatus новый статус встречи
     * @param oldStatus старый статус встречи
     * @param teamCardStatus статус карточки команды
     * @param teamGrade оценка команды
     * @param meetingLink ссылка на встречу
     */
    void updateTeamCardInfo(UUID teamCardId, UUID uuid, MeetingStatus newStatus,
                            MeetingStatus oldStatus, TeamCardStatus teamCardStatus,
                            BigDecimal teamGrade, String meetingLink);
}
