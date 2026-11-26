package net.trackme.backend.rest.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.messaging.TeamCardLowGradeSummaryEvent;
import net.trackme.backend.repos.WeeklyTeamReportRepository;
import net.trackme.backend.services.teamcard.WeeklyTeamReports;
import net.trackme.backend.services.teamcard.WeeklyTeamReports.Kind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

class WeeklyTeamReportControllerTest extends BaseApplicationTest {
    private static final String REPORTS = "/api/v1/admin/reports";

    @Autowired
    private WeeklyTeamReports weeklyTeamReports;

    @Autowired
    private WeeklyTeamReportRepository repository;

    @BeforeEach
    @AfterEach
    void clearReports() {
        repository.deleteAllInBatch();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void previousReportDownloadsArchivedRows() throws Exception {
        var csv = WeeklyTeamReports.lowGradeCsv(List.of(new TeamCardLowGradeSummaryEvent(
                "Команда", "Поток", new BigDecimal("0.20"), "Трекер")));
        weeklyTeamReports.archive(Kind.LOW_GRADE, Instant.now(), csv);

        mockMvc.perform(get(REPORTS + "/low-grade").param("period", "PREVIOUS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"low-grade-previous.csv\""))
                .andExpect(content().bytes(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void emptyArchiveIsDifferentFromMissingArchive() throws Exception {
        var request = get(REPORTS + "/missed-meetings").param("period", "PREVIOUS");
        mockMvc.perform(request).andExpect(status().isNotFound());

        weeklyTeamReports.archive(Kind.MISSED_MEETINGS, Instant.now(), "");

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "TRACKER")
    void trackerCannotDownloadReports() throws Exception {
        mockMvc.perform(get(REPORTS + "/low-grade")).andExpect(status().isForbidden());
        mockMvc.perform(get(REPORTS + "/missed-meetings")).andExpect(status().isForbidden());
    }
}
