package com.iimp.incident.controller;

import com.iimp.common.dto.ApiResponse;
import com.iimp.incident.domain.TimelineEvent;
import com.iimp.incident.dto.CreateIncidentRequest;
import com.iimp.incident.dto.DashboardSummary;
import com.iimp.incident.dto.IncidentResponse;
import com.iimp.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident Management API")
@SecurityRequirement(name = "bearerAuth")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SRE')")
    @Operation(summary = "Create a new incident")
    public ApiResponse<IncidentResponse> createIncident(
        @Valid @RequestBody CreateIncidentRequest request,
        @AuthenticationPrincipal String userId
    ) {
        return ApiResponse.ok(
            incidentService.createIncident(request, UUID.fromString(userId)),
            "Incident created successfully"
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "List all incidents (paginated)")
    public ApiResponse<Page<IncidentResponse>> listIncidents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(incidentService.listIncidents(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get incident by ID")
    public ApiResponse<IncidentResponse> getIncident(@PathVariable UUID id) {
        return ApiResponse.ok(incidentService.getById(id));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','SRE')")
    @Operation(summary = "Acknowledge an incident")
    public ApiResponse<IncidentResponse> acknowledge(
        @PathVariable UUID id,
        @AuthenticationPrincipal String userId
    ) {
        return ApiResponse.ok(incidentService.acknowledge(id, UUID.fromString(userId)));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','SRE')")
    @Operation(summary = "Resolve an incident")
    public ApiResponse<IncidentResponse> resolve(
        @PathVariable UUID id,
        @AuthenticationPrincipal String userId
    ) {
        return ApiResponse.ok(incidentService.resolve(id, UUID.fromString(userId)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','SRE')")
    @Operation(summary = "Close a resolved incident")
    public ApiResponse<IncidentResponse> close(@PathVariable UUID id) {
        return ApiResponse.ok(incidentService.close(id));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get incident timeline")
    public ApiResponse<List<TimelineEvent>> getTimeline(@PathVariable UUID id) {
        return ApiResponse.ok(incidentService.getTimeline(id));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SRE','DEVELOPER','VIEWER')")
    @Operation(summary = "Get dashboard summary metrics")
    public ApiResponse<DashboardSummary> getDashboardSummary() {
        return ApiResponse.ok(incidentService.getDashboardSummary());
    }
}
