package net.trackme.backend.services.teamcard.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Тесты генерации Excel-отчёта по карточкам команд.
 */
class TeamCardsReportExcelGeneratorTest {

  private final TeamCardsReportExcelGenerator generator =
      new TeamCardsReportExcelGenerator();

  @Test
  void generatesFilledRowForCompleteRecord() throws Exception {
    var record = TeamCardReportRecordDto.builder()
        .organizationName("Организация А")
        .streamId(UUID.randomUUID())
        .streamName("Поток")
        .startDate(LocalDate.of(2026, 1, 10))
        .endDate(LocalDate.of(2026, 6, 10))
        .teamCardName("Команда")
        .teamId(UUID.randomUUID())
        .username("tracker")
        .averageTeamGrade(BigDecimal.valueOf(4.5))
        .averageUserGrade(BigDecimal.valueOf(4.25))
        .meetingsCountPlan(10)
        .meetingsCountFact(8)
        .ntiMarkets(List.of("AeroNet", "AutoNet"))
        .readinessLevel("3-5")
        .build();
    var out = new ByteArrayOutputStream();

    generator.generate(Stream.of(record), out);

    assertThat(out.size()).isGreaterThan(0);
    try (Workbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(out.toByteArray()))) {
      var sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("Отчёт по командам");
      var row = sheet.getRow(2);
      assertThat(row.getCell(0).getStringCellValue()).isEqualTo("Организация А");
      assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Поток");
      assertThat(row.getCell(2).getStringCellValue()).isEqualTo("2026-01-10");
      assertThat(row.getCell(3).getStringCellValue()).isEqualTo("2026-06-10");
      assertThat(row.getCell(4).getStringCellValue()).isEqualTo("Команда");
      assertThat(row.getCell(5).getStringCellValue()).isEqualTo("tracker");
      assertThat(row.getCell(6).getNumericCellValue()).isEqualTo(4.5);
      assertThat(row.getCell(7).getNumericCellValue()).isEqualTo(4.25);
      assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(10);
      assertThat(row.getCell(9).getNumericCellValue()).isEqualTo(8);
      assertThat(row.getCell(10).getStringCellValue()).isEqualTo("AeroNet, AutoNet");
      assertThat(row.getCell(11).getStringCellValue()).isEqualTo("3-5");
    }
  }

  @Test
  void generatesEmptyCellsForIncompleteRecord() throws Exception {
    var record = TeamCardReportRecordDto.builder()
        .teamCardName("Команда")
        .username("tracker")
        .build();
    var out = new ByteArrayOutputStream();

    generator.generate(Stream.of(record), out);

    try (Workbook workbook = new XSSFWorkbook(
        new ByteArrayInputStream(out.toByteArray()))) {
      var row = workbook.getSheetAt(0).getRow(2);
      assertThat(row.getCell(2).getStringCellValue()).isEmpty();
      assertThat(row.getCell(3).getStringCellValue()).isEmpty();
      assertThat(row.getCell(10).getStringCellValue()).isEmpty();
    }
  }
}
