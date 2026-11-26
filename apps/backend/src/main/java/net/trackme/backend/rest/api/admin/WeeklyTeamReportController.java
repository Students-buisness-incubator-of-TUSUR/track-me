package net.trackme.backend.rest.api.admin;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.trackme.backend.services.teamcard.TeamCardSummaryService;
import net.trackme.backend.services.teamcard.WeeklyTeamReports;
import net.trackme.backend.services.teamcard.WeeklyTeamReports.Kind;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class WeeklyTeamReportController {
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private final TeamCardSummaryService summaries;
    private final WeeklyTeamReports reports;
    private final RestClient meetings;

    public WeeklyTeamReportController(TeamCardSummaryService summaries, WeeklyTeamReports reports,
            RestClient.Builder client, @Value("${MEETING_SERVICE_URL:http://trackme-meeting-service:8082}") String meetingUrl) {
        this.summaries = summaries;
        this.reports = reports;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.meetings = client.baseUrl(meetingUrl).requestFactory(factory).build();
    }

    public enum Period {
        CURRENT, PREVIOUS
    }

    @GetMapping("/low-grade")
    public ResponseEntity<byte[]> lowGrade(@RequestParam(defaultValue = "CURRENT") Period period) {
        return download(period, Kind.LOW_GRADE, "low-grade", () ->
                WeeklyTeamReports.lowGradeCsv(summaries.buildLowGradeSummary()));
    }

    @GetMapping("/missed-meetings")
    public ResponseEntity<byte[]> missedMeetings(@RequestParam(defaultValue = "CURRENT") Period period,
            @AuthenticationPrincipal Jwt jwt) {
        return download(period, Kind.MISSED_MEETINGS, "missed-meetings", () ->
                WeeklyTeamReports.missedMeetingsCsv(summaries.buildMissedMeetingSummary(fetchMissedMeetings(jwt))));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> reportError(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .cacheControl(CacheControl.noStore())
                .body(exception.getBody());
    }

    private List<Map<String, String>> fetchMissedMeetings(Jwt jwt) {
        try {
            var rows = meetings.get().uri("/api/v1/admin/reports/missed-meetings")
                    .headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
                    .retrieve().body(new ParameterizedTypeReference<List<Map<String, String>>>() { });
            if (rows != null) {
                return rows;
            }
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Не удалось получить данные о пропущенных встречах", exception);
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Сервис встреч вернул пустой ответ");
    }

    private ResponseEntity<byte[]> download(Period period, Kind kind, String filename, Supplier<String> current) {
        var csv = period == Period.CURRENT ? current.get() : reports.previous(kind)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Сводка за прошлую неделю ещё не сохранена"));
        if (csv.isEmpty()) {
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename + "-" + period.name().toLowerCase(Locale.ROOT) + ".csv").build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
