package net.trackme.backend.services.teamcard;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;

public interface TeamCardsReportService {
    /**
     * Экспортировать отчёт по карточкам команд в Excel-файл,
     * записывая результат напрямую в переданный поток.
     *
     * @param records      стрим записей отчёта для выгрузки
     * @param outputStream поток вывода для записи xlsx
     * @throws IOException в случае ошибки при генерации файла
     */
    void exportToExcel(Stream<TeamCardReportRecordDto> records, OutputStream outputStream) throws IOException;
}
