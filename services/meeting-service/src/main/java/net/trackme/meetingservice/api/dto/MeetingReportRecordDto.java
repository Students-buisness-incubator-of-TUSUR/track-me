package net.trackme.meetingservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "Запись отчета о встречах")
public record MeetingReportRecordDto(
    @Schema(description = "Название команды, для которой была встрча")
    String teamName,

    @Schema(description = "Дата начала встречи")
    OffsetDateTime startDate,

    @Schema(description = "Имя трекера, который ответственен за встречу")
    String trackerName,

    @Schema(description = "ФИО трекера, который ответственен за встречу.")
    String trackerFullName,

    @Schema(description = "Задачи на следующей встрече")
    String tasksNextMeeting,

    @Schema(description = "Задачи на текущую встречу")
    String tasksCurrentMeeting,

    @Schema(description = "Статус команды на встрече")
    TeamStatus teamStatus,

    @Schema(description = "Статус встречи")
    MeetingStatus status
) {
}
