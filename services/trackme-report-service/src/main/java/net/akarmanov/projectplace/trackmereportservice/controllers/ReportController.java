package net.akarmanov.projectplace.trackmereportservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/reports")
@Tag(name = "Report API", description = "API для генерации отчетов")
public interface ReportController {
    @Operation(summary = "Генерация отчета в формате Excel",
            description = "Генерация отчета в формате Excel")
    @GetMapping(path = "/main/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ResponseEntity<Resource> generateExcelReport(Authentication authentication);
}
