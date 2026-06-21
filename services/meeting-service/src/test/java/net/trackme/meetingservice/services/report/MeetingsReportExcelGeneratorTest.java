package net.trackme.meetingservice.services.report;

import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MeetingsReportExcelGeneratorTest {

    private final MeetingsReportExcelGenerator generator = new MeetingsReportExcelGenerator();

    @Test
    void generate_createsValidExcelFileWithData() throws Exception {
        String streamName = "Тестовый Поток 2024";
        UUID teamId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        var record = new MeetingReportRecordDto(
                teamId,
                "Команда А",
                OffsetDateTime.parse("2024-05-10T10:00:00Z"),
                "tracker_username",
                "Иван Трекеров",
                "Выполнено",
                "Не выполнено",
                TeamStatus.OK,
                MeetingStatus.COMPLETED
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(streamName, Stream.of(record), out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            assertEquals(1, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Встречи", sheet.getSheetName());

            Row titleRow = sheet.getRow(0);
            assertNotNull(titleRow);
            assertEquals("Отчёт по встречам на потоке: Тестовый Поток 2024", titleRow.getCell(0).getStringCellValue());

            Row headerRow = sheet.getRow(1);
            assertEquals("Название команды", headerRow.getCell(0).getStringCellValue());
            assertEquals("Статус команды", headerRow.getCell(5).getStringCellValue());

            Row dataRow = sheet.getRow(2);
            assertNotNull(dataRow);
            assertEquals("Команда А", dataRow.getCell(0).getStringCellValue());
            assertEquals("10.05.2024", dataRow.getCell(1).getStringCellValue());
            assertEquals("Иван Трекеров", dataRow.getCell(2).getStringCellValue());
            assertEquals("Выполнено", dataRow.getCell(3).getStringCellValue());
            assertEquals("Не выполнено", dataRow.getCell(4).getStringCellValue());
            assertEquals("Всё ок", dataRow.getCell(5).getStringCellValue());
        }
    }

    @Test
    void generate_handlesCancelledMeetingsCorrectly() throws Exception {
        UUID teamId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

        var record = new MeetingReportRecordDto(
                teamId,
                "Команда",
                OffsetDateTime.parse("2024-05-10T10:00:00Z"),
                "tracker_username",
                "Иван Трекеров",
                null,
                null,
                null,
                MeetingStatus.COMPLETED_AS_NOT_HAPPENED
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate("Test", Stream.of(record), out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Row dataRow = workbook.getSheetAt(0).getRow(2);
            assertEquals("—", dataRow.getCell(3).getStringCellValue());
            assertEquals("Не состоялась", dataRow.getCell(5).getStringCellValue());
        }
    }

    @Test
    void generate_withMultipleRecords_createsMultipleRows() throws Exception {
        UUID teamId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID teamId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

        var record1 = new MeetingReportRecordDto(
                teamId1,
                "Команда 1",
                OffsetDateTime.parse("2024-05-10T10:00:00Z"),
                "tracker1",
                "Трекер 1",
                "Задача 1",
                "Задача 2",
                TeamStatus.OK,
                MeetingStatus.COMPLETED
        );

        var record2 = new MeetingReportRecordDto(
                teamId2,
                "Команда 2",
                OffsetDateTime.parse("2024-05-11T10:00:00Z"),
                "tracker2",
                "Трекер 2",
                "Задача 3",
                "Задача 4",
                TeamStatus.WITH_ISSUES,
                MeetingStatus.COMPLETED
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate("Test", Stream.of(record1, record2), out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(4, sheet.getPhysicalNumberOfRows()); // title + header + 2 data rows
            assertNotNull(sheet.getRow(2));
            assertNotNull(sheet.getRow(3));
        }
    }

    @Test
    void generate_withEmptyStream_createsOnlyTitleAndHeader() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate("Empty Stream", Stream.empty(), out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getPhysicalNumberOfRows()); // только title + header
            assertNotNull(sheet.getRow(0));
            assertNotNull(sheet.getRow(1));
            assertNull(sheet.getRow(2));
        }
    }

    @Test
    void generate_withNullFields_handlesGracefully() throws Exception {
        UUID teamId = UUID.randomUUID();

        var record = new MeetingReportRecordDto(
                teamId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate("Test", Stream.of(record), out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(2);
            assertNotNull(dataRow);
            // Проверяем, что нет NullPointerException
            dataRow.getCell(0);
        }
    }

    @Test
    void generate_handlesScheduledStatus() throws Exception {
        UUID teamId = UUID.randomUUID();

        var record = new MeetingReportRecordDto(
                teamId,
                "Команда",
                OffsetDateTime.parse("2024-05-10T10:00:00Z"),
                "tracker",
                "Трекер",
                null,
                null,
                null,
                MeetingStatus.SCHEDULED
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate("Test", Stream.of(record), out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Row dataRow = workbook.getSheetAt(0).getRow(2);
            assertEquals("—", dataRow.getCell(3).getStringCellValue());
            assertEquals("—", dataRow.getCell(4).getStringCellValue());
            assertEquals("Запланирована", dataRow.getCell(5).getStringCellValue());
        }
    }
}