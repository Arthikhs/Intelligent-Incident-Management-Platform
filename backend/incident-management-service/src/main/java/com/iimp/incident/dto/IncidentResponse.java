package com.iimp.incident.dto;

import com.iimp.incident.domain.Incident;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IncidentResponse {
    private UUID id;
    private String title;
    private String description;
    private Incident.Severity severity;
    private Incident.IncidentStatus status;
    private List<String> affectedServices;
    private int alertCount;
    private int errorCount;
    private long impactedUsers;
    private Long mttrSeconds;
    private Instant detectedAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
