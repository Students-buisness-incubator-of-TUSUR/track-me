package net.akarmanov.projectplace.commons.filters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FilterRequest(
    @Valid
    @NotNull(message = "Фильтры не могут быть пустыми")
    List<@Valid Filter> filters
) {
}
