package net.trackme.meetingservice.services.report;

import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PropertyTemplate;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Базовый класс для генераторов Excel-отчётов по встречам.
 * Содержит общую логику: заголовок, шапка, стили, разбивка на группы.
 */
public abstract class AbstractMeetingsReportExcelGenerator {

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Возвращает заголовки колонок (разные для обычного и трекерского отчёта). */
    protected abstract String[] getHeaders();

    /** Возвращает текст заголовка листа. */
    protected abstract String getTitleText(String streamName);

    /**
     * Записывает специфичные колонки строки данных (задачи, трекер, статус).
     * Вызывается из writeData после общих колонок "Название" и "Дата".
     */
    protected abstract void writeRecordRow(Row row, Object reportRecord, Styles styles);

    /**
     * Записывает общие для обоих отчётов колонки: название команды (0) и дату (1).
     */
    protected void writeCommonFirstColumns(Row row, String teamName, OffsetDateTime startDate,
                                           Styles styles) {
        setString(row, 0, teamName, styles.text);
        String dateStr = startDate != null ? startDate.format(DATE_FORMATTER) : null;
        setString(row, 1, dateStr, styles.text);
    }

    /**
     * Записывает ячейку статуса с учётом MeetingStatus.
     */
    protected void writeStatusCell(Row row, int col, TeamStatus teamStatus,
                            MeetingStatus status, Styles styles) {
        var statusCell = row.createCell(col);
        if (status == null) {
            statusCell.setCellValue("—");
            statusCell.setCellStyle(styles.text);
            return;
        }
        switch (status) {
            case SCHEDULED -> {
                statusCell.setCellValue("Запланирована");
                statusCell.setCellStyle(styles.statusLavender);
            }
            case COMPLETED_AS_NOT_HAPPENED -> {
                statusCell.setCellValue("Не состоялась");
                statusCell.setCellStyle(styles.textCancelled);
            }
            default -> {
                statusCell.setCellValue(mapTeamStatusToText(teamStatus));
                statusCell.setCellStyle(getStyleByTeamStatus(teamStatus, styles));
            }
        }
    }

    /**
     * Записывает ячейки с задачами в колонки nextCol (следующая) и currentCol (текущая).
     * Для запланированных и несостоявшихся — прочерки.
     */
    protected void writeTaskCells(Row row, MeetingStatus status,
                                  String tasksNext, String tasksCurrent,
                                  int nextCol, int currentCol, Styles styles) {
        boolean isScheduled = status == MeetingStatus.SCHEDULED;
        boolean isNotHappened = status == MeetingStatus.COMPLETED_AS_NOT_HAPPENED;

        CellStyle baseStyle = isNotHappened ? styles.textCancelled : styles.text;
        CellStyle wrapStyle = isNotHappened ? styles.textCancelledWrap : styles.textWrap;

        if (isScheduled || isNotHappened) {
            setString(row, nextCol, "—", baseStyle);
            setString(row, currentCol, "—", baseStyle);
        } else {
            setString(row, nextCol, tasksNext, wrapStyle);
            setString(row, currentCol, tasksCurrent, wrapStyle);
        }
    }

    protected void writeTitle(Sheet sheet, Styles styles, String streamName) {
        var row = sheet.createRow(0);
        row.setHeightInPoints(30);
        var cell = row.createCell(0);
        cell.setCellValue(getTitleText(streamName));
        cell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, getHeaders().length - 1));
    }

    protected void writeHeaders(Sheet sheet, Styles styles) {
        String[] headers = getHeaders();
        var row = sheet.createRow(1);
        row.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            var cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
        }
    }

    protected void updateGroupBorder(PropertyTemplate pt, String[] lastTeamName,
                                     int[] groupStartRow, int currentRowNum, String teamName) {
        if (lastTeamName[0] != null && !Objects.equals(lastTeamName[0], teamName)) {
            applyGroupBorder(pt, groupStartRow[0], currentRowNum - 1);
            groupStartRow[0] = currentRowNum;
        }
        lastTeamName[0] = teamName;
    }

    protected void applyGroupBorder(PropertyTemplate pt, int firstRow, int lastRow) {
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, 0, getHeaders().length - 1);
        pt.drawBorders(region, BorderStyle.MEDIUM, IndexedColors.BLACK.getIndex(), BorderExtent.OUTSIDE);
    }

    protected String mapTeamStatusToText(TeamStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case OK -> "Всё ок";
            case WITH_ISSUES -> "Есть проблемы";
            case MANY_ISSUES -> "Большие проблемы";
        };
    }

    protected CellStyle getStyleByTeamStatus(TeamStatus status, Styles styles) {
        if (status == null) return styles.text;
        return switch (status) {
            case OK -> styles.statusGreen;
            case WITH_ISSUES -> styles.statusYellow;
            case MANY_ISSUES -> styles.statusRed;
        };
    }

    protected void setString(Row row, int col, String value, CellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue((value == null || value.isBlank()) ? "—" : value);
        cell.setCellStyle(style);
    }

    /**
     * Общие стили для обоих отчётов. Вынесено сюда, чтобы не дублировать ~70 строк.
     */
    protected static class Styles {
            protected final CellStyle title;
            protected final CellStyle header;
            protected final CellStyle text;
            protected final CellStyle textWrap;
            protected final CellStyle textCancelled;
            protected final CellStyle textCancelledWrap;
            protected final CellStyle statusGreen;
            protected final CellStyle statusYellow;
            protected final CellStyle statusRed;
            protected final CellStyle statusLavender;

        protected Styles(SXSSFWorkbook wb) {
            title = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setupCentered(title);

            header = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setupCentered(header);
            setupBorders(header, BorderStyle.THIN);

            text = wb.createCellStyle();
            setupCentered(text);
            setupBorders(text, BorderStyle.THIN);

            textWrap = wb.createCellStyle();
            setupCentered(textWrap);
            setupBorders(textWrap, BorderStyle.THIN);
            textWrap.setWrapText(true);

            textCancelled = createColoredStyle(wb, IndexedColors.GREY_25_PERCENT);
            textCancelledWrap = createColoredStyle(wb, IndexedColors.GREY_25_PERCENT);
            textCancelledWrap.setWrapText(true);

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
