package net.trackme.backend.services.teamcard.report;

import lombok.RequiredArgsConstructor;

import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.commons.filters.Filter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static net.trackme.backend.domain.spec.TeamCardSpecification.*;
import static net.trackme.backend.domain.spec.TeamCardSpecification.hasStream;

@Service
@RequiredArgsConstructor
public class TeamCardsReportServiceImpl implements TeamCardsReportService {
    private final TeamCardsReportExcelGenerator excelGenerator;

    private final TeamCardsRepository teamCardsRepository;

    private final TeamCardMapper teamCardMapper;

    @Override
    public List<TeamCardReportRecordDto> getReportRecords(List<Filter> filters) {
        var queryContext = prepareReportQueryContext(filters);
        var allCards = teamCardsRepository.findAll(queryContext.fetchSpec());
        return mapAndEnrichToDto(allCards, queryContext.gradeByUser());
    }

    @Override
    public Page<TeamCardReportRecordDto> getReportRecords(List<Filter> filters, Pageable pageable) {
        var queryContext = prepareReportQueryContext(filters);

        Page<TeamCard> idPage = teamCardsRepository.findAll(queryContext.baseSpec(), pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        var ids = idPage.getContent().stream().map(TeamCard::getId).toList();
        var teamCards = teamCardsRepository.findAll(
                queryContext.fetchSpec().and(idIn(ids)),
                pageable.getSort()
        );

        var enrichedContent = mapAndEnrichToDto(teamCards, queryContext.gradeByUser());
        return new PageImpl<>(enrichedContent, pageable, idPage.getTotalElements());
    }

    @Override
    public void streamRecordsToExcel(
            List<Filter> filters,
            int fetchPageSize,
            int exportLimit,
            OutputStream outputStream
    ) throws IOException {
        var queryContext = prepareReportQueryContext(filters);
        var recordStream = IntStream.iterate(0, i -> i + 1)
                .mapToObj(page -> {
                    var idPage = teamCardsRepository.findAll(queryContext.baseSpec(), PageRequest.of(page, fetchPageSize));
                    if (idPage.isEmpty()) {
                        return List.<TeamCardReportRecordDto>of();
                    }

                    var ids = idPage.getContent().stream().map(TeamCard::getId).toList();
                    var fullTeamCards = teamCardsRepository.findAll(
                            queryContext.fetchSpec().and(idIn(ids)),
                            idPage.getSort()
                    );

                    return mapAndEnrichToDto(fullTeamCards, queryContext.gradeByUser());
                })
                .takeWhile(batch -> !batch.isEmpty())
                .flatMap(Collection::stream)
                .limit(exportLimit);

        excelGenerator.generate(recordStream, outputStream);
    }


    /**
     * Строит спецификацию для выборки карточек команд в отчёте.
     *
     * @param filters список фильтров от клиента
     * @return спецификация для запроса к БД
     */
    private Specification<TeamCard> buildTeamCardSpecForReport(List<Filter> filters) {
        return withFilters(filters).and(hasStream());
    }

    /**
     * Вспомогательный контекст для агрегации данных отчёта.
     * Хранит спецификацию для выборки карточек и мапу средних оценок.
     *
     * @param fetchSpec   спецификация для выборки {@link TeamCard} с необходимыми связями (Fetch JOIN)
     * @param gradeByUser мапа, где ключ — имя пользователя (трекера), значение — средняя оценка его команд
     */
    private record ReportQueryContext(
            Specification<TeamCard> baseSpec,
            Specification<TeamCard> fetchSpec,
            Map<String, Double> gradeByUser
    ) {}

    /**
     * Подготавливает контекст для выполнения запросов к БД при формировании отчёта.
     * Сначала строит базовую спецификацию, затем на её основе рассчитывает средние оценки,
     * и в конце добавляет условия для загрузки связанных сущностей (решение проблемы N+1).
     *
     * @param filters список фильтров, переданных клиентом
     * @return объект {@link ReportQueryContext}, содержащий готовую спецификацию и рассчитанные оценки
     */
    private ReportQueryContext prepareReportQueryContext(List<Filter> filters) {
        var baseSpec = buildTeamCardSpecForReport(filters);
        var gradeByUser = teamCardsRepository.getAverageGradesByUser(baseSpec);
        var fetchSpec = baseSpec.and(withFetchJoins());
        return new ReportQueryContext(baseSpec, fetchSpec, gradeByUser);
    }

    /**
     * Преобразует коллекцию сущностей {@link TeamCard} в список DTO для отчёта
     * и обогащает каждую запись средней оценкой пользователя (трекера).
     *
     * @param teamCards   коллекция сущностей карточек команд
     * @param gradeByUser мапа со средними оценками трекеров (рассчитывается заранее для всей выборки)
     * @return список готовых к отправке клиенту {@link TeamCardReportRecordDto}
     */
    private List<TeamCardReportRecordDto> mapAndEnrichToDto(
            Collection<TeamCard> teamCards,
            Map<String, Double> gradeByUser) {

        return teamCards.stream()
                .map(teamCardMapper::mapToReportDto)
                .map(record -> applyUserGrade(record, gradeByUser))
                .toList();
    }

    /**
     * Применяет рассчитанную среднюю оценку пользователя к записи отчёта.
     * Если оценка для пользователя не найдена, возвращает запись без изменений.
     *
     * @param record      запись отчёта
     * @param gradeByUser map: username → средняя оценка
     * @return запись с заполненным полем {@code averageUserGrade}, либо исходная запись
     */
    private TeamCardReportRecordDto applyUserGrade(TeamCardReportRecordDto record,
                                                   Map<String, Double> gradeByUser) {
        var userGrade = gradeByUser.get(record.username());
        return userGrade != null
                ? record.withAverageUserGrade(BigDecimal.valueOf(userGrade)
                .setScale(2, RoundingMode.HALF_UP))
                : record;
    }
}