package net.trackme.backend.services.teamcard.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.commons.filters.Filter;
import org.junit.jupiter.api.BeforeEach;
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

  private final OrganizationContext organizationContext = mock(OrganizationContext.class);

  private final TeamCardsReportServiceImpl service = new TeamCardsReportServiceImpl(
      excelGenerator, teamCardsRepository, teamCardMapper, organizationContext);

  @BeforeEach
  void setUp() {
    when(organizationContext.getOrganizationId()).thenReturn(Optional.empty());
  }

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

  @Test
  @SuppressWarnings("unchecked")
  void getReportRecordsAppliesOrganizationFilterForRegularUser() {
    var organizationId = UUID.randomUUID();
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(organizationId));
    when(organizationContext.isOverlord()).thenReturn(false);
    when(teamCardsRepository.getAverageGradesByUser(any(Specification.class)))
        .thenReturn(Map.of());
    when(teamCardsRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of());

    service.getReportRecords(List.of());

    var specCaptor = ArgumentCaptor.forClass(Specification.class);
    verify(teamCardsRepository).findAll(specCaptor.capture(), any(Sort.class));

    var criteriaBuilder = mock(CriteriaBuilder.class);
    var root = mock(Root.class);
    var organizationPath = mock(Path.class);
    var predicate = mock(Predicate.class);
    when(root.get("organizationId")).thenReturn(organizationPath);
    when(criteriaBuilder.equal(organizationPath, organizationId)).thenReturn(predicate);
    when(criteriaBuilder.isNotEmpty(any(Expression.class))).thenReturn(predicate);
    when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
    when(criteriaBuilder.conjunction()).thenReturn(predicate);

    specCaptor.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);

    verify(criteriaBuilder).equal(organizationPath, organizationId);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getReportRecordsDoesNotApplyOrganizationFilterForOverlord() {
    var organizationId = UUID.randomUUID();
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(organizationId));
    when(organizationContext.isOverlord()).thenReturn(true);
    when(teamCardsRepository.getAverageGradesByUser(any(Specification.class)))
        .thenReturn(Map.of());
    when(teamCardsRepository.findAll(any(Specification.class), any(Sort.class)))
        .thenReturn(List.of());

    service.getReportRecords(List.of());

    var specCaptor = ArgumentCaptor.forClass(Specification.class);
    verify(teamCardsRepository).findAll(specCaptor.capture(), any(Sort.class));

    var criteriaBuilder = mock(CriteriaBuilder.class);
    var root = mock(Root.class);
    var predicate = mock(Predicate.class);
    when(criteriaBuilder.isNotEmpty(any(Expression.class))).thenReturn(predicate);
    when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
    when(criteriaBuilder.conjunction()).thenReturn(predicate);

    specCaptor.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);

    verify(root, never()).get("organizationId");
  }

  private static TeamCardReportRecordDto record(String username) {
    return TeamCardReportRecordDto.builder()
        .teamId(UUID.randomUUID())
        .teamCardName("Команда")
        .username(username)
        .build();
  }
}
