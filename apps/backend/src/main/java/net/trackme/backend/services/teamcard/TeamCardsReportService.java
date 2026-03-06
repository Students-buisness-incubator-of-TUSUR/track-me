package net.trackme.backend.services.teamcard;

import java.io.IOException;
import java.util.List;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;

public interface TeamCardsReportService {
    /**
     * Экспортировать отчет в excel-формат.
     * @param records Список карточек команд
     * @return Список карточек команд
     */
    public byte[] exportToExcel(List<TeamCardReportRecordDto> records) throws IOException;
}
