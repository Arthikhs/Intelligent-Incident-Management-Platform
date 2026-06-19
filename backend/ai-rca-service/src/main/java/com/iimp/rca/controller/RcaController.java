package com.iimp.rca.controller;

import com.iimp.common.dto.ApiResponse;
import com.iimp.rca.domain.RcaReport;
import com.iimp.rca.service.AiRcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rca")
@RequiredArgsConstructor
@Tag(name = "AI RCA", description = "AI-powered Root Cause Analysis API")
@SecurityRequirement(name = "bearerAuth")
public class RcaController {

    private final AiRcaService aiRcaService;

    @GetMapping("/incidents/{incidentId}")
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get latest RCA report for an incident")
    public ApiResponse<RcaReport> getLatestRca(@PathVariable UUID incidentId) {
        return ApiResponse.ok(aiRcaService.getByIncidentId(incidentId));
    }

    @GetMapping("/incidents/{incidentId}/all")
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get all RCA reports for an incident")
    public ApiResponse<List<RcaReport>> getAllRca(@PathVariable UUID incidentId) {
        return ApiResponse.ok(aiRcaService.getAllByIncidentId(incidentId));
    }
}
