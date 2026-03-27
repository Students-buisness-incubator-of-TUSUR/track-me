package net.trackme.backend.rest.api.teamcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "DTO для записи отчета")
public record TeamCardReportRecordDto(
        @Schema(description = "Имя потока")
        String streamName,
        @Schema(description = "Дата начала потока")
        LocalDate startDate,
        @Schema(description = "Дата окончания потока")
        LocalDate endDate,
        @Schema(description = "Название карточки команды")
        String teamCardName,
        @Schema(description = "Имя пользователя, которому принадлежит карточка команды")
        String username,
        @Schema(description = "Средняя оценка команды")
        BigDecimal averageTeamGrade,
        @Schema(description = "Средняя оценка пользователя")
        BigDecimal averageUserGrade,
        @Schema(description = "Количество встреч (план)")
        Integer meetingsCountPlan,
        @Schema(description = "Количество встреч (факт)")
        Integer meetingsCountFact,
        @Schema(description = "Рынки НТИ")
        List<String> ntiMarkets,
        @Schema(description = "Уровень готовности технологии",
                allowableValues = {"0-2", "3-5", "6-8", "9-10"})
        String readinessLevel
) {
    public TeamCardReportRecordDto withAverageUserGrade(BigDecimal averageUserGrade) {
        return new TeamCardReportRecordDto(
                streamName,
                startDate,
                endDate,
                teamCardName,
                username,
                averageTeamGrade,
                averageUserGrade,
                meetingsCountPlan,
                meetingsCountFact,
                ntiMarkets,
                readinessLevel
        );
    }

}
