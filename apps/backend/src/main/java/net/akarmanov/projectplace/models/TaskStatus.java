package net.akarmanov.projectplace.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статусы задач")
public enum TaskStatus {
    @Schema(description = "Новая задача")
    NEW,
    @Schema(description = "Задача в работе")
    IN_PROGRESS,
    @Schema(description = "Задача завершена")
    DONE,
    @Schema(description = "Задача просрочена")
    OVERDUE,
    @Schema(description = "Задача не выполнена")
    NOT_DONE
}
