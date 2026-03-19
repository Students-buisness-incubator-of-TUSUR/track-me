package net.trackme.backend.usecases;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.exceptions.ExcelExportException;
import net.trackme.backend.services.stream.MutableStreamService;
import net.trackme.backend.services.teamcard.TeamCardsReportService;
import net.trackme.backend.services.teamcard.TeamCardsService;
import net.trackme.commons.filters.Filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.trackme.backend.domain.spec.TeamCardSpecification.*;

@Component
@RequiredArgsConstructor
public class TeamCardsUseCase {

    private final TeamCardsService teamCardsService;

    private final MutableStreamService streamService;

    private final TeamCardsReportService teamCardsReportService;

    private final TeamCardMapper teamCardMapper;

    private final NtiMarketService ntiMarketService;

    @Value("${app.report.export-limit:10000}")
    private int exportLimit;

    @Value("${app.report.fetch-page-size:500}")
    private int fetchPageSize;

    @Transactional
    public TeamCardDto createTeamCard(TeamCardCreateDto teamCardDto, UUID streamId,
                                      Authentication authentication) {
        var username = authentication.getName();
        var ntiMarketIds = teamCardDto.ntiMarketIds();

        var stream = streamService.findActive(streamId);
        var teamCardEntity = teamCardMapper.mapToEntity(teamCardDto);
        var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

        teamCardEntity.setUsername(username);
        teamCardEntity.setNtiMarkets(ntiMarkets);
        teamCardEntity.addStream(stream);
        teamCardEntity.setStatus(TeamCardStatus.OK);

        var createdTeamCard = teamCardsService.createTeamCard(teamCardEntity);
        return teamCardMapper.mapToDto(createdTeamCard);
    }


    public TeamCardDto updateTeamCard(UUID teamCardId,
                                      TeamCardUpdateDto createOrUpdateDto) {
        var ntiMarketIds = createOrUpdateDto.ntiMarketIds();
        var teamCard = teamCardMapper.mapToEntity(createOrUpdateDto);
        var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

        teamCard.setNtiMarkets(ntiMarkets);
        var updatedTeamCard = teamCardsService.updateTeamCard(teamCardId, teamCard);
        return teamCardMapper.mapToDto(updatedTeamCard);
    }

    public Page<TeamCardDto> getTeamCards(List<Filter> filters,
                                          Authentication authentication,
                                          Pageable pageable) {
        var page = teamCardsService.getTeamCardsPageable(
                withFilters(filters)
                        .and(userEquals(authentication.getName())),
                pageable);
        return page.map(teamCardMapper::mapToDto);
    }

    @PreAuthorize("hasPermission(#teamCardId, 'net.trackme.backend.domain.TeamCard', 'READ')")
    public TeamCardDto getTeamCard(UUID teamCardId) {
        var teamCard = teamCardsService.getTeamCard(teamCardId);
        return teamCardMapper.mapToDto(teamCard);
    }

    public void deleteTeamCard(UUID id) {
        teamCardsService.deleteTeamCard(id);
    }

    public Integer getTeamCardCount(UUID streamId) {
        return teamCardsService.getTeamCardCount(streamId);
    }

    public Page<TeamCardReportRecordDto> getTeamCardReport(List<Filter> filters,
                                                           Pageable pageable) {
        var teamCardSpec = withFilters(filters).and(withFetchJoins()).and(hasStream());
        var teamCardPage = teamCardsService.getTeamCardsPageable(teamCardSpec, pageable);
        return teamCardPage.map(teamCardMapper::mapToReportDto);
    }

    public void streamTeamCardReportExcel(List<Filter> filters, OutputStream outputStream) {
        var spec = withFilters(filters).and(withFetchJoins()).and(hasStream());
        var recordStream = IntStream.iterate(0, i -> i + 1)
                .mapToObj(page -> teamCardsService
                        .getTeamCardsPageable(spec, PageRequest.of(page, fetchPageSize))
                        .getContent())
                .takeWhile(batch -> !batch.isEmpty())
                .flatMap(Collection::stream)
                .limit(exportLimit)
                .map(teamCardMapper::mapToReportDto);

        try {
            teamCardsReportService.exportToExcel(recordStream, outputStream);
        } catch (IOException e) {
            throw new ExcelExportException("Ошибка генерации Excel отчёта", e);
        }
    }
}
