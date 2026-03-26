package net.trackme.backend.services.teamcard;

import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

@Service
public class TeamCardsReportServiceImpl implements TeamCardsReportService {
    private static final String SHEET_NAME = "Отчёт по командам";
    private static final String[] HEADERS = {
            "Поток",
            "Дата начала",
            "Дата окончания",
            "Название команды",
            "Трекер",
            "Средняя оценка команды",
            "Средняя оценка пользователя",
            "Встреч (план)",
            "Встреч (факт)",
            "Рынки НТИ",
            "Уровень TRL"
    };

    @Override
    public void exportToExcel(Stream<TeamCardReportRecordDto> records, OutputStream outputStream) throws IOException {
        try (var workbook = new SXSSFWorkbook(500)) {
            workbook.setCompressTempFiles(true);

            var sheet  = workbook.createSheet(SHEET_NAME);
            sheet.setDefaultColumnWidth(20);
            sheet.trackAllColumnsForAutoSizing();

            var styles = new Styles(workbook);
            writeTitle(sheet, styles);
            writeHeaders(sheet, styles);
            writeData(sheet, records, styles);

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                int newWidth = sheet.getColumnWidth(i) + 1024;
                sheet.setColumnWidth(i, Math.min(newWidth, 255 * 256));
            }

            workbook.write(outputStream);
        }
    }

    private void writeTitle(SXSSFSheet sheet, Styles styles) {
        var titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        var titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Отчёт по карточкам команд");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
    }

    private void writeHeaders(SXSSFSheet sheet, Styles styles) {
        var headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(20);
        for (int i = 0; i < HEADERS.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private void writeData(SXSSFSheet sheet, Stream<TeamCardReportRecordDto> records, Styles styles) {
        var rowNum = new int[]{2};
        records.forEach(record -> {
            var row = sheet.createRow(rowNum[0]++);
            row.setHeightInPoints(18);

            setString(row, 0,
                    record.streamName(),
                    styles.text
            );
            setString(row, 1,
                    record.startDate() != null ? record.startDate().toString() : "",
                    styles.text
            );
            setString(row, 2,
                    record.endDate() != null ? record.endDate().toString() : "",
                    styles.text
            );
            setString(row, 3,
                    record.teamCardName(),
                    styles.text
            );
            setString(row, 4,
                    record.username(),
                    styles.text
            );
            setNumber(row, 5,
                    record.averageTeamGrade() != null ? record.averageTeamGrade().doubleValue() : null,
                    styles.number
            );
            setNumber(row, 6,
                    record.averageUserGrade() != null ? record.averageUserGrade().doubleValue() : null,
                    styles.number
            );
            setInteger(row, 7,
                    record.meetingsCountPlan(),
                    styles.integer
            );
            setInteger(row, 8,
                    record.meetingsCountFact(),
                    styles.integer
            );
            setString(row, 9,
                    record.ntiMarkets() != null ? String.join(", ", record.ntiMarkets()) : "",
                    styles.text
            );
            setString(row, 10,
                    record.readinessLevel(),
                    styles.text
            );
        });
    }

    private void setString(Row row, int col, String value, CellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setNumber(Row row, int col, Double value, CellStyle style) {
        var cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void setInteger(Row row, int col, Integer value, CellStyle style) {
        var cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    /**
     * Контейнер стилей
     */
    private static class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle text;
        final CellStyle number;
        final CellStyle integer;

        Styles(SXSSFWorkbook wb) {
            var titleFont = wb.createFont();
            titleFont.setFontName("Arial");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            var headerFont = wb.createFont();
            headerFont.setFontName("Arial");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);

            var regularFont = wb.createFont();
            regularFont.setFontName("Arial");
            regularFont.setFontHeightInPoints((short) 11);

            title = wb.createCellStyle();
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            header = wb.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);
            header.setWrapText(true);

            text = wb.createCellStyle();
            text.setFont(regularFont);
            text.setAlignment(HorizontalAlignment.CENTER);
            text.setVerticalAlignment(VerticalAlignment.CENTER);
            text.setBorderBottom(BorderStyle.THIN);
            text.setBorderLeft(BorderStyle.THIN);
            text.setBorderRight(BorderStyle.THIN);

            var numberFormat = wb.createDataFormat();
            number = wb.createCellStyle();
            number.setFont(regularFont);
            number.setAlignment(HorizontalAlignment.CENTER);
            number.setVerticalAlignment(VerticalAlignment.CENTER);
            number.setDataFormat(numberFormat.getFormat("0.00"));
            number.setBorderBottom(BorderStyle.THIN);
            number.setBorderLeft(BorderStyle.THIN);
            number.setBorderRight(BorderStyle.THIN);

            integer = wb.createCellStyle();
            integer.cloneStyleFrom(number);
            integer.setDataFormat(numberFormat.getFormat("0"));
        }
    }
}