package com.iimp.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "deployments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(nullable = false, length = 50)
    private String environment;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "deployed_by", length = 200)
    private String deployedBy;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(length = 200)
    private String branch;

    @Column(name = "pipeline_url", length = 1000)
    private String pipelineUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_changes", columnDefinition = "jsonb")
    private Map<String, Object> configChanges;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "rolled_back_at")
    private Instant rolledBackAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
