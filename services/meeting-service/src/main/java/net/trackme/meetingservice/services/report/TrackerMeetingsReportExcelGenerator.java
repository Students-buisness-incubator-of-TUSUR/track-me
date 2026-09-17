package net.trackme.meetingservice.services.report;

import net.trackme.meetingservice.api.dto.TrackerMeetingReportRecordDto;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.PropertyTemplate;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

@Component
public class TrackerMeetingsReportExcelGenerator extends AbstractMeetingsReportExcelGenerator {

    private static final String[] HEADERS = {
            "Название команды",
            "Дата встречи",
            "Задачи к следующей встрече",
            "Выполнили задачи прошлой встречи или нет, общая информация по команде",
            "Статус команды"
    };

    @Override
    protected String[] getHeaders() {
        return HEADERS;
    }

    @Override
    protected String getTitleText(String streamName) {
        return "Отчёт трекера по встречам";
    }

    @Override
    protected void writeRecordRow(Row row, Object reportRecord, Styles styles) {
        var dto = (TrackerMeetingReportRecordDto) reportRecord;
        MeetingStatus status = dto.status();

        writeCommonFirstColumns(row, dto.teamName(), dto.startDate(), status, styles);
        writeTaskCells(row, status, dto.tasksNextMeeting(), dto.tasksCurrentMeeting(), 2, 3, styles);
        writeStatusCell(row, 4, dto.teamStatus(), status, styles);
    }

    public void generate(
            Stream<TrackerMeetingReportRecordDto> records,
            OutputStream outputStream
    ) throws IOException {
        try (var workbook = new SXSSFWorkbook(500)) {
            workbook.setCompressTempFiles(true);
            var sheet = workbook.createSheet("Встречи");
            prepareSheet(sheet);
            var styles = new Styles(workbook);

            writeTitle(sheet, styles, null);
            writeHeaders(sheet, styles);
            writeData(sheet, records, styles);

            workbook.write(outputStream);
        }
    }

    private void writeData(Sheet sheet, Stream<TrackerMeetingReportRecordDto> records, Styles styles) {
        final int[] rowTracker = {2};
        final int[] groupStartRow = {2};
        final String[] lastTeamName = {null};

        PropertyTemplate pt = new PropertyTemplate();

        records.forEach(reportRecord -> {
            int currentRowNum = rowTracker[0]++;
            var row = sheet.createRow(currentRowNum);
            writeRecordRow(row, reportRecord, styles);
            updateGroupBorder(pt, lastTeamName, groupStartRow, currentRowNum, reportRecord.teamName());
        });

        if (rowTracker[0] > 2) {
            applyGroupBorder(pt, groupStartRow[0], rowTracker[0] - 1);
            pt.applyBorders(sheet);
        }
    }

    private void prepareSheet(Sheet sheet) {
        sheet.setColumnWidth(0, 25 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 40 * 256);
        sheet.setColumnWidth(3, 75 * 256);
        sheet.setColumnWidth(4, 20 * 256);
    }
}
