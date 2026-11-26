package net.trackme.backend.repos;

import net.trackme.backend.domain.WeeklyTeamReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyTeamReportRepository extends JpaRepository<WeeklyTeamReport, String> {
}
