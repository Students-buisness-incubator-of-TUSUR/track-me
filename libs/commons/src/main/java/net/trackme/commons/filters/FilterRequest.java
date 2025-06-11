package net.trackme.commons.filters;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Запрос фильтров для поиска
 *
 * @param filters список фильтров
 */
@Schema(name = "FilterRequest", description = "Запрос фильтров для поиска")
public record FilterRequest(
    @Valid
    @NotNull(message = "Фильтры не могут быть пустыми")
    @Schema(description = "Список фильтров для поиска", requiredMode = REQUIRED)
    List<@Valid Filter> filters
) {
}
