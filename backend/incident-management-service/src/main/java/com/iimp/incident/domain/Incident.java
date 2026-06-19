package com.iimp.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "affected_services", columnDefinition = "text[]")
    @Builder.Default
    private List<String> affectedServices = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "source_alert_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private List<UUID> sourceAlertIds = new ArrayList<>();

    @Column(name = "deployment_id")
    private UUID deploymentId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private Instant detectedAt = Instant.now();

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "mttr_seconds")
    private Long mttrSeconds;

    @Column(name = "alert_count")
    @Builder.Default
    private int alertCount = 0;

    @Column(name = "error_count")
    @Builder.Default
    private int errorCount = 0;

    @Column(name = "impacted_users")
    @Builder.Default
    private long impactedUsers = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void resolve() {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        if (this.detectedAt != null) {
            this.mttrSeconds = Instant.now().getEpochSecond() - detectedAt.getEpochSecond();
        }
    }

    public void acknowledge() {
        this.status = IncidentStatus.INVESTIGATING;
        this.acknowledgedAt = Instant.now();
    }

    public void close() {
        this.status = IncidentStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }
    public enum IncidentStatus { OPEN, INVESTIGATING, MITIGATED, RESOLVED, CLOSED }
}
