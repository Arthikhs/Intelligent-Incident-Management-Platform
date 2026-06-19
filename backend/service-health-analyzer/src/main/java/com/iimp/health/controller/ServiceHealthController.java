package com.iimp.health.controller;

import com.iimp.common.dto.ApiResponse;
import com.iimp.health.domain.ServiceHealth;
import com.iimp.health.service.ServiceHealthAnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Service Health", description = "Service Health Monitoring API")
public class ServiceHealthController {

    private final ServiceHealthAnalyzerService healthService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get latest health for all services")
    public ApiResponse<List<ServiceHealth>> getAllHealth() {
        return ApiResponse.ok(healthService.getLatestForAllServices());
    }

    @GetMapping("/{serviceName}")
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get latest health for a specific service")
    public ApiResponse<ServiceHealth> getServiceHealth(@PathVariable String serviceName) {
        return ApiResponse.ok(healthService.getLatestForService(serviceName));
    }
}
