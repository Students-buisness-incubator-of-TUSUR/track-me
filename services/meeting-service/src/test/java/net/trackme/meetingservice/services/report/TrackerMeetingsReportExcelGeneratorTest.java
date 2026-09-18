package net.trackme.meetingservice.services.report;

import net.trackme.meetingservice.api.dto.TrackerMeetingReportRecordDto;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
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

class TrackerMeetingsReportExcelGeneratorTest {

    private final TrackerMeetingsReportExcelGenerator generator =
            new TrackerMeetingsReportExcelGenerator();

    private static final UUID TEAM_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    // ========== Базовые тесты ==========

    @Test
    void generate_createsValidExcelFileWithData() throws Exception {
        var reportRecord = TrackerMeetingReportRecordDto.builder()
                .teamId(TEAM_ID)
                .teamName("Команда А")
                .startDate(OffsetDateTime.parse("2024-05-10T10:00:00Z"))
                .tasksNextMeeting("Выполнено")
                .tasksCurrentMeeting("Не выполнено")
                .teamStatus(TeamStatus.OK)
                .status(MeetingStatus.COMPLETED)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.of(reportRecord), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(1, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Встречи", sheet.getSheetName());

            Row titleRow = sheet.getRow(0);
            assertNotNull(titleRow);
            assertEquals("Отчёт трекера по встречам", titleRow.getCell(0).getStringCellValue());

            Row headerRow = sheet.getRow(1);
            assertEquals("Название команды", headerRow.getCell(0).getStringCellValue());
            assertEquals("Дата встречи", headerRow.getCell(1).getStringCellValue());
            assertEquals("Задачи к следующей встрече", headerRow.getCell(2).getStringCellValue());
            assertEquals("Выполнили задачи прошлой встречи или нет, общая информация по команде",
                    headerRow.getCell(3).getStringCellValue());
            assertEquals("Статус команды", headerRow.getCell(4).getStringCellValue());

            Row dataRow = sheet.getRow(2);
            assertNotNull(dataRow);
            assertEquals("Команда А", dataRow.getCell(0).getStringCellValue());
            assertEquals("10.05.2024", dataRow.getCell(1).getStringCellValue());
            assertEquals("Не выполнено", dataRow.getCell(2).getStringCellValue());
            assertEquals("Выполнено", dataRow.getCell(3).getStringCellValue());
            assertEquals("Всё ок", dataRow.getCell(4).getStringCellValue());
        }
    }

    @Test
    void generate_withMultipleRecords_createsMultipleRows() throws Exception {
        var record1 = TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName("Команда 1")
                .startDate(OffsetDateTime.parse("2024-05-10T10:00:00Z"))
                .tasksNextMeeting("Задача 1")
                .tasksCurrentMeeting("Задача 2")
                .teamStatus(TeamStatus.OK)
                .status(MeetingStatus.COMPLETED)
                .build();

        var record2 = TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName("Команда 2")
                .startDate(OffsetDateTime.parse("2024-05-11T10:00:00Z"))
                .tasksNextMeeting("Задача 3")
                .tasksCurrentMeeting("Задача 4")
                .teamStatus(TeamStatus.WITH_ISSUES)
                .status(MeetingStatus.COMPLETED)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.of(record1, record2), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(4, sheet.getPhysicalNumberOfRows());
            assertNotNull(sheet.getRow(2));
            assertNotNull(sheet.getRow(3));
            assertEquals("Команда 1", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("Команда 2", sheet.getRow(3).getCell(0).getStringCellValue());
        }
    }

    @Test
    void generate_withEmptyStream_createsOnlyTitleAndHeader() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.empty(), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getPhysicalNumberOfRows());
            assertNotNull(sheet.getRow(0));
            assertNotNull(sheet.getRow(1));
            assertNull(sheet.getRow(2));
        }
    }

    @Test
    void generate_withNullFields_handlesGracefully() throws Exception {
        var reportRecord = TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName(null)
                .startDate(null)
                .tasksNextMeeting(null)
                .tasksCurrentMeeting(null)
                .teamStatus(null)
                .status(null)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.of(reportRecord), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row dataRow = sheet.getRow(2);
            assertNotNull(dataRow);
            assertEquals("—", dataRow.getCell(0).getStringCellValue());
            assertEquals("—", dataRow.getCell(1).getStringCellValue());
            assertNotNull(dataRow.getCell(2));
            assertNotNull(dataRow.getCell(3));
            assertNotNull(dataRow.getCell(4));
        }
    }

    // ========== Статусы ==========

    @Test
    void generate_handlesScheduledStatus() throws Exception {
        var reportRecord = TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName("Команда")
                .startDate(OffsetDateTime.parse("2024-05-10T10:00:00Z"))
                .tasksNextMeeting(null)
                .tasksCurrentMeeting(null)
                .teamStatus(null)
                .status(MeetingStatus.SCHEDULED)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.of(reportRecord), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Row dataRow = workbook.getSheetAt(0).getRow(2);
            assertEquals("—", dataRow.getCell(2).getStringCellValue());
            assertEquals("—", dataRow.getCell(3).getStringCellValue());
            assertEquals("Запланирована", dataRow.getCell(4).getStringCellValue());
        }
    }

    @Test
    void generate_handlesCancelledMeeting_marksAllCommonCellsAsCancelled() throws Exception {
        var reportRecord = TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName("Отменённая команда")
                .startDate(OffsetDateTime.parse("2024-05-10T10:00:00Z"))
                .tasksNextMeeting(null)
                .tasksCurrentMeeting(null)
                .teamStatus(null)
                .status(MeetingStatus.COMPLETED_AS_NOT_HAPPENED)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.of(reportRecord), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Row dataRow = workbook.getSheetAt(0).getRow(2);
            assertEquals("—", dataRow.getCell(2).getStringCellValue());
            assertEquals("—", dataRow.getCell(3).getStringCellValue());
            assertEquals("Не состоялась", dataRow.getCell(4).getStringCellValue());

            // ⚠️ Проверка проблемы №1:
            // team/date ячейки должны иметь тот же fill color, что и task-ячейки (серый)
            Cell teamCell = dataRow.getCell(0);
            Cell dateCell = dataRow.getCell(1);
            Cell taskNextCell = dataRow.getCell(2);

            assertNotNull(teamCell.getCellStyle());
            assertNotNull(dateCell.getCellStyle());
            assertNotNull(taskNextCell.getCellStyle());

            short teamFill = teamCell.getCellStyle().getFillForegroundColor();
            short dateFill = dateCell.getCellStyle().getFillForegroundColor();
            short taskFill = taskNextCell.getCellStyle().getFillForegroundColor();

            assertEquals(taskFill, teamFill,
                    "team cell fill color must match task cell fill color for cancelled meeting");
            assertEquals(taskFill, dateFill,
                    "date cell fill color must match task cell fill color for cancelled meeting");

            // Серый цвет (GREY_25_PERCENT = 22 в IndexedColors)
            assertEquals(IndexedColors.GREY_25_PERCENT.getIndex(), teamFill,
                    "cancelled meeting should use grey fill color");
        }
    }

    @Test
    void generate_handlesCompletedMeeting_usesNormalStyleForCommonCells() throws Exception {
        var reportRecord = TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName("Обычная команда")
                .startDate(OffsetDateTime.parse("2024-05-10T10:00:00Z"))
                .tasksNextMeeting("Задача")
                .tasksCurrentMeeting("Задача 2")
                .teamStatus(TeamStatus.OK)
                .status(MeetingStatus.COMPLETED)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.of(reportRecord), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Row dataRow = workbook.getSheetAt(0).getRow(2);

            Cell teamCell = dataRow.getCell(0);

            // Для COMPLETED — team-ячейка НЕ должна быть серой
            short teamFill = teamCell.getCellStyle().getFillForegroundColor();
            assertNotEquals(IndexedColors.GREY_25_PERCENT.getIndex(), teamFill,
                    "team cell should not use cancelled grey fill for COMPLETED status");
        }
    }

    // ========== Статусы команды ==========

    @Test
    void generate_handlesAllTeamStatuses() throws Exception {
        var statuses = new TeamStatus[]{TeamStatus.OK, TeamStatus.WITH_ISSUES, TeamStatus.MANY_ISSUES};
        var expected = new String[]{"Всё ок", "Есть проблемы", "Большие проблемы"};

        for (int i = 0; i < statuses.length; i++) {
            var reportRecord = TrackerMeetingReportRecordDto.builder()
                    .teamId(UUID.randomUUID())
                    .teamName("Команда")
                    .startDate(OffsetDateTime.parse("2024-05-10T10:00:00Z"))
                    .tasksNextMeeting("t")
                    .tasksCurrentMeeting("c")
                    .teamStatus(statuses[i])
                    .status(MeetingStatus.COMPLETED)
                    .build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            generator.generate(Stream.of(reportRecord), out);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
                Row dataRow = workbook.getSheetAt(0).getRow(2);
                assertEquals(expected[i], dataRow.getCell(4).getStringCellValue(),
                        "Wrong label for status " + statuses[i]);
            }
        }
    }

    // ========== Колонки ==========

    @Test
    void generate_setsColumnWidths() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.empty(), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i < 5; i++) {
                assertTrue(sheet.getColumnWidth(i) > 0,
                        "Column " + i + " width should be set");
            }
        }
    }

    @Test
    void generate_titleSpanMergedAcrossAllColumns() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(Stream.empty(), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getMergedRegions().size());
            var merged = sheet.getMergedRegions().get(0);
            assertEquals(0, merged.getFirstColumn());
            assertEquals(4, merged.getLastColumn());
        }
    }
}
