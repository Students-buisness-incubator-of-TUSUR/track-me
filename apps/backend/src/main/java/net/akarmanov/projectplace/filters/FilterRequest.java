package net.akarmanov.projectplace.filters;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Фильтры для запросов поиска")
public record FilterRequest(
    @Valid
    @Schema(description = "Фильтры",
            implementation = Filter.class)
    @NotNull(message = "Фильтры не могут быть пустыми")
    List<@Valid Filter> filters
) {
}
