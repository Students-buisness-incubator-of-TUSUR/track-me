package net.trackme.backend.services.teamcard.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.commons.filters.Filter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Тесты формирования отчёта по карточкам команд.
 */
class TeamCardsReportServiceImplTest {

  private final TeamCardsReportExcelGenerator excelGenerator =
      mock(TeamCardsReportExcelGenerator.class);

  private final TeamCardsRepository teamCardsRepository = mock(TeamCardsRepository.class);

  private final TeamCardMapper teamCardMapper = mock(TeamCardMapper.class);

  private final TeamCardsReportServiceImpl service = new TeamCardsReportServiceImpl(
      excelGenerator, teamCardsRepository, teamCardMapper);

  @Test
  void getReportRecordsUsesDefaultOrganizationSort() {
    var teamCard = new TeamCard();
    var record = record("tracker");
    when(teamCardsRepository.getAverageGradesByUser(any(Specification.class)))
        .thenReturn(Map.of("tracker", 4.456));
    when(teamCardsRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of(teamCard));
    when(teamCardMapper.mapToReportDto(teamCard)).thenReturn(record);

    var result = service.getReportRecords(List.<Filter>of());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().averageUserGrade()).isEqualByComparingTo("4.46");
  }

  @Test
  void getReportRecordsPageAddsDefaultOrganizationSort() {
    var pageable = PageRequest.of(0, 10);
    var teamCard = new TeamCard();
    when(teamCardsRepository.getAverageGradesByUser(any(Specification.class)))
        .thenReturn(Map.of());
    when(teamCardMapper.mapToReportDto(teamCard)).thenReturn(record("tracker"));
    when(teamCardsRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(teamCard), pageable, 1));

    var result = service.getReportRecords(List.<Filter>of(), pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().averageUserGrade()).isNull();
    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(teamCardsRepository)
        .findAll(any(Specification.class), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Direction.ASC, "organizationName"));
  }

  @Test
  void getReportRecordsPageKeepsExplicitSort() {
    var pageable = PageRequest.of(0, 10, Sort.by("teamCardName"));
    var teamCard = new TeamCard();
    when(teamCardsRepository.getAverageGradesByUser(any(Specification.class)))
        .thenReturn(Map.of());
    when(teamCardMapper.mapToReportDto(teamCard)).thenReturn(record("tracker"));
    when(teamCardsRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(teamCard), pageable, 1));

    service.getReportRecords(List.<Filter>of(), pageable);

    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(teamCardsRepository)
        .findAll(any(Specification.class), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getSort()).isEqualTo(pageable.getSort());
  }

  @Test
  void getReportRecordsPageReturnsEmptyPageWithoutDefaultSort() {
    var pageable = Pageable.unpaged();
    when(teamCardsRepository.getAverageGradesByUser(any(Specification.class)))
        .thenReturn(Map.of());
    when(teamCardsRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty(pageable));

    Page<TeamCardReportRecordDto> result = service.getReportRecords(List.of(), pageable);

    assertThat(result).isEmpty();
  }

  private static TeamCardReportRecordDto record(String username) {
    return TeamCardReportRecordDto.builder()
        .teamId(UUID.randomUUID())
        .teamCardName("Команда")
        .username(username)
        .build();
  }
}
