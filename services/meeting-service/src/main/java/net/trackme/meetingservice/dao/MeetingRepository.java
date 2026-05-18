package net.trackme.meetingservice.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;

/**
 * Репозиторий для работы с сущностями Meeting.
 * Предоставляет методы для доступа к данным встреч.
 */
public interface MeetingRepository extends JpaRepository<Meeting, UUID>, JpaSpecificationExecutor<Meeting> {

    /**
     * Находит встречи по статусу и дате начала до указанной даты.
     *
     * @param status статус встречи
     * @param before дата, до которой нужно найти встречи (включительно)
     * @param pageable параметры пагинации
     * @return список встреч, соответствующих критериям
     */
    List<Meeting> findByStatusAndStartDateBefore(
            MeetingStatus status,
            OffsetDateTime before,
            Pageable pageable);

    /**
     * Проверяет существование встречи для команды в указанном временном диапазоне.
     *
     * @param teamCardId идентификатор карточки команды
     * @param from начало временного диапазона (включительно)
     * @param to конец временного диапазона (исключительно)
     * @return true если встреча существует, false в противном случае
     */
    boolean existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
            UUID teamCardId,
            OffsetDateTime from,
            OffsetDateTime to);

    /**
     * Проверяет существование встречи для команды в указанном временном диапазоне,
     * исключая встречу с указанным ID.
     *
     * @param teamCardId идентификатор карточки команды
     * @param from начало временного диапазона (включительно)
     * @param to конец временного диапазона (исключительно)
     * @param excludeId ID встречи, которую нужно исключить из проверки
     * @return true если существует другая встреча, false в противном случае
     */
    boolean existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThanAndIdNot(
            UUID teamCardId,
            OffsetDateTime from,
            OffsetDateTime to,
            UUID excludeId);

    /**
     * Находит все встречи, соответствующие спецификации, с указанной сортировкой.
     *
     * @param spec спецификация для фильтрации
     * @param sort параметры сортировки
     * @return список встреч, соответствующих критериям
     */
    @Override
    List<Meeting> findAll(Specification<Meeting> spec, Sort sort);
}
