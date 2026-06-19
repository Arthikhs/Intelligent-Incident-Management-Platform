package com.iimp.incident.controller;

import com.iimp.common.dto.ApiResponse;
import com.iimp.incident.domain.Alert;
import com.iimp.incident.repository.AlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert Management API")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertRepository alertRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "List alerts by status")
    public ApiResponse<List<Alert>> listAlerts(
        @RequestParam(defaultValue = "FIRING") String status
    ) {
        List<Alert> alerts = "ALL".equalsIgnoreCase(status)
            ? alertRepository.findAll()
            : alertRepository.findByStatusOrderByFiredAtDesc(status.toUpperCase());
        return ApiResponse.ok(alerts);
    }
}
