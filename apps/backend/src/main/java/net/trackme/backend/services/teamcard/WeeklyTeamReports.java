package net.trackme.backend.services.teamcard;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.WeeklyTeamReport;
import net.trackme.backend.messaging.TeamCardLowGradeSummaryEvent;
import net.trackme.backend.messaging.TeamCardSummaryEvent;
import net.trackme.backend.repos.WeeklyTeamReportRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyTeamReports {
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Tomsk");
    private final WeeklyTeamReportRepository reports;
    private final PlatformTransactionManager transactions;

    public enum Kind {
        LOW_GRADE, MISSED_MEETINGS
    }

    public Optional<String> previous(Kind kind) {
        return reports.findById(key(kind, previousMonday(Instant.now()))).map(WeeklyTeamReport::getCsv);
    }

    public void archive(Kind kind, Instant runAt, String csv) {
        var key = key(kind, previousMonday(runAt));
        var transaction = new TransactionTemplate(transactions);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transaction.executeWithoutResult(status -> {
                if (!reports.existsById(key)) {
                    reports.save(new WeeklyTeamReport(key, csv));
                }
            });
        } catch (DataAccessException | TransactionException exception) {
            log.error("Could not archive weekly report {}", key, exception);
        }
    }

    private static LocalDate previousMonday(Instant runAt) {
        return runAt.atZone(REPORT_ZONE).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
    }

    private static String key(Kind kind, LocalDate week) {
        return kind.name() + ":" + week;
    }

    public static String lowGradeCsv(List<TeamCardLowGradeSummaryEvent> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        var csv = new StringJoiner("\r\n", "\uFEFF", "\r\n");
        csv.add("Команда;Поток;Трекер;Средний рейтинг");
        rows.forEach(row -> csv.add(line(row.teamCardName(), row.streamName(),
                row.trackerFullName(), row.averageGrade().toPlainString())));
        return csv.toString();
    }

    public static String missedMeetingsCsv(List<TeamCardSummaryEvent> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        var csv = new StringJoiner("\r\n", "\uFEFF", "\r\n");
        csv.add("Команда;Поток;Трекер;Номер встречи;Ссылка");
        rows.forEach(row -> csv.add(line(row.teamCardName(), row.streamName(),
                row.trackerFullName(), row.meetingNumber(), row.meetingLink())));
        return csv.toString();
    }

    private static String line(String... cells) {
        var line = new StringJoiner(";");
        for (String cell : cells) {
            var value = cell == null ? "" : cell;
            var trimmed = value.stripLeading();
            if ((!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0)
                    || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) {
                value = "'" + value;
            }
            line.add("\"" + value.replace("\"", "\"\"") + "\"");
        }
        return line.toString();
    }
}
