package net.trackme.backend.rest.api.teamcard;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.usecases.TeamCardsUseCase;
import net.trackme.commons.filters.FilterRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamCardsRestControllerImpl implements TeamCardsRestController {

    private final TeamCardsUseCase teamCardsUseCase;

    @Override
    public ResponseEntity<TeamCardDto> createTeamCard(UUID streamId, TeamCardCreateDto dto,
                                                      Authentication authentication) {
        var createTeamCardDto = teamCardsUseCase.createTeamCard(dto, streamId, authentication);
        return ResponseEntity.ok(createTeamCardDto);
    }

    @Override
    public ResponseEntity<TeamCardDto> updateTeamCard(UUID teamCardId,
                                                      TeamCardUpdateDto dto) {
        var updatedTeamCard = teamCardsUseCase.updateTeamCard(teamCardId, dto);
        return ResponseEntity.ok(updatedTeamCard);
    }

    @Override
    public ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(FilterRequest filterRequest,
                                                                Pageable pageable,
                                                                Authentication authentication) {
        var page = teamCardsUseCase.getTeamCards(filterRequest.filters(), authentication, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @Override
    public ResponseEntity<TeamCardDto> getTeamCard(UUID id) {
        var teamCardDto = teamCardsUseCase.getTeamCard(id);
        return ResponseEntity.ok(teamCardDto);
    }

    @Override
    public ResponseEntity<Integer> getTeamCardCount(UUID streamId) {
        var count = teamCardsUseCase.getTeamCardCount(streamId);
        return ResponseEntity.ok(count);
    }

    @Override
    public ResponseEntity<Void> deleteTeamCard(UUID id) {
        teamCardsUseCase.deleteTeamCard(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedModel<TeamCardReportRecordDto>> getTeamCardReport(
            FilterRequest filters,
            Pageable pageable) {
        var page = teamCardsUseCase.getTeamCardReport(filters.filters(), pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StreamingResponseBody> getTeamCardReportExcel(FilterRequest filters) {
        String filename = "отчёт-по-командам-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                ".xlsx";

        String filenameEncoded = URLEncoder
                .encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        StreamingResponseBody body = outputStream ->
                teamCardsUseCase.streamTeamCardReportExcel(filters.filters(), outputStream);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + filenameEncoded
            )
            .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .body(body);
    }
}
