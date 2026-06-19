package com.iimp.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String incidentId;
    private String title;
    private String severity;       // CRITICAL, HIGH, MEDIUM, LOW
    private String status;         // OPEN, INVESTIGATING, MITIGATED, RESOLVED, CLOSED
    private List<String> affectedServices;
    private String eventType;      // CREATED, UPDATED, RESOLVED, CLOSED

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt = Instant.now();
}
