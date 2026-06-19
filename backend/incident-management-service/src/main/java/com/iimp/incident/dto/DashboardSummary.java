package com.iimp.incident.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardSummary {
    private long totalIncidents;
    private long activeIncidents;
    private long criticalIncidents;
    private long highIncidents;
    private double avgMttrHours;
    private Map<String, Long> incidentsByService;
    private Map<String, Long> incidentsByStatus;
    private Map<String, Long> incidentsBySeverity;
}
