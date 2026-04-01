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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MeetingsReportExcelGeneratorTest {

    private final MeetingsReportExcelGenerator generator = new MeetingsReportExcelGenerator();

    @Test
    void generate_createsValidExcelFileWithData() throws Exception {
        // Arrange
        String streamName = "Тестовый Поток 2024";
        var record = new MeetingReportRecordDto(
                "Команда А",
                OffsetDateTime.parse("2024-05-10T10:00:00Z"),
                "Иван Трекеров",
                "Иван Трекеров",
                "Выполнено",
                "Не выполнено",
                TeamStatus.OK,
                MeetingStatus.COMPLETED
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Act
        generator.generate(streamName, Stream.of(record), out);

        // Assert: Читаем сгенерированный Excel из памяти
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {

            assertEquals(1, workbook.getNumberOfSheets(), "Должен быть создан 1 лист");
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
        // Arrange
        var record = new MeetingReportRecordDto(
                "Команда",
                OffsetDateTime.parse("2024-05-10T10:00:00Z"),
                "Иван Трекеров",
                "Сделать задачу 1",
                null,
                null,
                null,
                MeetingStatus.COMPLETED_AS_NOT_HAPPENED
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Act
        generator.generate("Test", Stream.of(record), out);

        // Assert
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Row dataRow = workbook.getSheetAt(0).getRow(2);
            assertEquals("—", dataRow.getCell(3).getStringCellValue(), "Для несостоявшихся должен быть прочерк");
            assertEquals("Не состоялась", dataRow.getCell(5).getStringCellValue());
        }
    }
}