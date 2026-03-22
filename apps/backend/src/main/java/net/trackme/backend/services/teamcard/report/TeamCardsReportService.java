package net.trackme.backend.services.teamcard.report;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.commons.filters.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamCardsReportService {

    /**
     * Получить полный список записей отчёта по карточкам команд с обогащением
     *
     * @param filters список фильтров для выборки карточек команд
     * @return обогащённый список записей отчёта
     */
    List<TeamCardReportRecordDto> getReportRecords(List<Filter> filters);

    /**
     * Получить пагинированный список записей отчёта по карточкам команд
     *
     * @param filters  список фильтров для выборки карточек команд
     * @param pageable параметры пагинации
     * @return страница обогащённых записей отчёта
     */
    Page<TeamCardReportRecordDto> getReportRecords(List<Filter> filters, Pageable pageable);

    /**
     * Формирует отчёт по карточкам команд в формате Excel и записывает его в поток вывода.
     * @param filters       список критериев фильтрации для выборки карточек команд
     * @param fetchPageSize количество записей, загружаемых из базы данных за один сетевой запрос
     * @param exportLimit   максимально допустимое общее количество записей в итоговом отчёте
     * @param outputStream  целевой поток вывода, в который будет записан сгенерированный Excel-файл (.xlsx)
     * @throws IOException если возникла ошибка ввода-вывода при формировании документа или записи в поток
     */
    void streamRecordsToExcel(List<Filter> filters, int fetchPageSize, int exportLimit, OutputStream outputStream)
            throws IOException;
}
