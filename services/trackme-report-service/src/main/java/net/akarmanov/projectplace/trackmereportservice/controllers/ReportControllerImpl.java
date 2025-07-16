package net.akarmanov.projectplace.trackmereportservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportControllerImpl implements ReportController {
    @Override
    public ResponseEntity<Resource> generateExcelReport(Authentication authentication) {
        return ResponseEntity.ok().build();
    }
}
