package net.trackme.meetingservice.services.report;

import net.trackme.commons.filters.Filter;
import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для формирования аналитической отчетности по встречам.
 */
public interface MeetingsReportService {

    /**
     * Возвращает полный список записей отчета для указанного потока.
     *
     * @param streamId идентификатор потока, по которому требуется построить отчет.
     * @param filters  список критериев фильтрации (например, по команде, трекеру или статусу).
     * @return список объектов {@link MeetingReportRecordDto}, соответствующих заданным условиям.
     */
    List<MeetingReportRecordDto> getReportRecordsForStream(
        UUID streamId,
        List<Filter> filters
    );

    /**
     * Возвращает пагинированную страницу записей отчета для указанного потока.
     * Используется для отображения данных в веб-интерфейсе с поддержкой сортировки.
     *
     * @param streamId идентификатор потока.
     * @param filters  список критериев фильтрации.
     * @param pageable параметры пагинации (номер страницы, размер) и правила сортировки.
     * @return объект {@link Page}, содержащий срез данных и метаданные пагинации.
     */
    Page<MeetingReportRecordDto> getReportRecordsForStream(
        UUID streamId,
        List<Filter> filters,
        Pageable pageable
    );

    /**
     * Формирует отчет в формате Excel и записывает его напрямую в выходной поток.
     * Метод реализован с использованием потоковой обработки данных для минимизации
     * потребления оперативной памяти при выгрузке больших объемов информации.
     *
     * @param streamId      идентификатор потока.
     * @param filters       список критериев фильтрации.
     * @param sort   параметры сортировки.
     * @param fetchPageSize количество записей, загружаемых из базы данных за одну итерацию.
     * @param exportLimit   максимально допустимое количество записей в итоговом файле.
     * @param outputStream  целевой поток записи (например, поток ответа HTTP).
     * @throws IOException если в процессе записи в поток произошла ошибка ввода-вывода.
     */
    void streamRecordsToExcelForStream(
        UUID streamId,
        List<Filter> filters,
        Sort sort,
        int fetchPageSize,
        int exportLimit,
        OutputStream outputStream
    ) throws IOException;
}
