package net.trackme.meetingservice.services.report;

import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PropertyTemplate;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class MeetingsReportExcelGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final String[] HEADERS = {
            "Название команды",
            "Дата встречи",
            "Трекер",
            "Задачи к следующей встрече",
            "Выполнили задачи прошлой встречи или нет, общая информация по команде",
            "Статус команды"
    };

    public void generate(
            String streamName,
            Stream<MeetingReportRecordDto> records,
            OutputStream outputStream
    ) throws IOException {
        try (var workbook = new SXSSFWorkbook(500)) {
            workbook.setCompressTempFiles(true);

            var sheet = workbook.createSheet("Встречи");

            sheet.setColumnWidth(0, 25 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 25 * 256);
            sheet.setColumnWidth(3, 40 * 256);
            sheet.setColumnWidth(4, 75 * 256);
            sheet.setColumnWidth(5, 20 * 256);

            var styles = new Styles(workbook);
            writeTitle(sheet, styles, streamName);
            writeHeaders(sheet, styles);

            writeData(sheet, records, styles);
            workbook.write(outputStream);
        }
    }

    private void writeTitle(Sheet sheet, Styles styles, String streamName) {
        var row = sheet.createRow(0);
        row.setHeightInPoints(30);
        var cell = row.createCell(0);
        cell.setCellValue("Отчёт по встречам на потоке: " + streamName);
        cell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
    }

    private void writeHeaders(Sheet sheet, Styles styles) {
        var row = sheet.createRow(1);
        row.setHeightInPoints(25);
        for (int i = 0; i < HEADERS.length; i++) {
            var cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private void writeData(Sheet sheet, Stream<MeetingReportRecordDto> records, Styles styles) {
        final int[] rowTracker = {2};
        final int[] groupStartRow = {2};
        final String[] lastTeamName = {null};

        PropertyTemplate pt = new PropertyTemplate();

        records.forEach(record -> {
            int currentRowNum = rowTracker[0]++;
            var row = sheet.createRow(currentRowNum);

            MeetingStatus mStatus = record.status();
            boolean isScheduled = mStatus == MeetingStatus.SCHEDULED;
            boolean isNotHappened = mStatus == MeetingStatus.COMPLETED_AS_NOT_HAPPENED;

            CellStyle rowBaseStyle = isNotHappened ? styles.textCancelled : styles.text;
            CellStyle rowWrapStyle = isNotHappened ? styles.textCancelledWrap : styles.textWrap;

            // Название команды
            setString(row, 0, record.teamName(), rowBaseStyle);

            // Дата
            String dateStr = record.startDate() != null ? record.startDate().format(DATE_FORMATTER) : null;
            setString(row, 1, dateStr, rowBaseStyle);

            // Трекер
            setString(row, 2, record.trackerFullName(), rowBaseStyle);

            // Прочерки для запланированных и несостоявшихся
            if (isScheduled || isNotHappened) {
                setString(row, 3, "—", rowBaseStyle);
                setString(row, 4, "—", rowBaseStyle);
            } else {
                setString(row, 3, record.tasksNextMeeting(), rowWrapStyle);
                setString(row, 4, record.tasksCurrentMeeting(), rowWrapStyle);
            }

            // Статус команды
            var statusCell = row.createCell(5);
            if (isScheduled) {
                statusCell.setCellValue("Запланирована");
                statusCell.setCellStyle(styles.statusLavender);
            } else if (isNotHappened) {
                statusCell.setCellValue("Не состоялась");
                statusCell.setCellStyle(styles.textCancelled);
            } else {
                statusCell.setCellValue(mapTeamStatusToText(record.teamStatus()));
                statusCell.setCellStyle(getStyleByTeamStatus(record.teamStatus(), styles));
            }

            // Логика группировки (границы)
            if (lastTeamName[0] != null && !Objects.equals(lastTeamName[0], record.teamName())) {
                applyGroupBorder(pt, groupStartRow[0], currentRowNum - 1);
                groupStartRow[0] = currentRowNum;
            }

            lastTeamName[0] = record.teamName();
        });

        if (rowTracker[0] > 2) {
            applyGroupBorder(pt, groupStartRow[0], rowTracker[0] - 1);
            pt.applyBorders(sheet);
        }
    }

    private void applyGroupBorder(PropertyTemplate pt, int firstRow, int lastRow) {
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, 0, HEADERS.length - 1);
        pt.drawBorders(region, BorderStyle.MEDIUM, IndexedColors.BLACK.getIndex(), BorderExtent.OUTSIDE);
    }

    private String mapTeamStatusToText(TeamStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case OK -> "Всё ок";
            case WITH_ISSUES -> "Есть проблемы";
            case MANY_ISSUES -> "Большие проблемы";
        };
    }

    private CellStyle getStyleByTeamStatus(TeamStatus status, Styles styles) {
        if (status == null) return styles.text;
        return switch (status) {
            case OK -> styles.statusGreen;
            case WITH_ISSUES -> styles.statusYellow;
            case MANY_ISSUES -> styles.statusRed;
        };
    }

    private void setString(Row row, int col, String value, CellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue((value == null || value.isBlank()) ? "—" : value);
        cell.setCellStyle(style);
    }

    private static class Styles {
        final CellStyle title, header, text, textWrap;
        final CellStyle textCancelled, textCancelledWrap;
        final CellStyle statusGreen, statusYellow, statusRed, statusLavender;

        Styles(SXSSFWorkbook wb) {
            Font font = wb.createFont();
            font.setFontHeightInPoints((short) 10);

            // Заголовок
            title = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setupCentered(title);

            // Шапка
            header = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setupCentered(header);
            setupBorders(header, BorderStyle.THIN);

            // Обычные данные
            text = wb.createCellStyle();
            setupCentered(text);
            setupBorders(text, BorderStyle.THIN);

            textWrap = wb.createCellStyle();
            setupCentered(textWrap);
            setupBorders(textWrap, BorderStyle.THIN);
            textWrap.setWrapText(true);

            // Стили для строки "Не состоялась"
            textCancelled = createColoredStyle(wb, IndexedColors.GREY_25_PERCENT);
            textCancelledWrap = createColoredStyle(wb, IndexedColors.GREY_25_PERCENT);
            textCancelledWrap.setWrapText(true);

            // Цветные статусы
            statusGreen = createColoredStyle(wb, IndexedColors.LIGHT_GREEN);
            statusYellow = createColoredStyle(wb, IndexedColors.LIGHT_YELLOW);
            statusRed = createColoredStyle(wb, IndexedColors.RED);
            statusLavender = createColoredStyle(wb, IndexedColors.LAVENDER);

            Font whiteFont = wb.createFont();
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            statusRed.setFont(whiteFont);
        }

        private void setupCentered(CellStyle style) {
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
        }

        private void setupBorders(CellStyle style, BorderStyle border) {
            style.setBorderBottom(border);
            style.setBorderTop(border);
            style.setBorderLeft(border);
            style.setBorderRight(border);
        }

        private CellStyle createColoredStyle(SXSSFWorkbook wb, IndexedColors color) {
            var style = wb.createCellStyle();
            setupCentered(style);
            setupBorders(style, BorderStyle.THIN);
            style.setFillForegroundColor(color.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }
    }
}