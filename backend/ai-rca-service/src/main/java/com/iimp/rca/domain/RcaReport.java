package com.iimp.rca.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rca_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcaReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "generated_by", length = 100)
    @Builder.Default
    private String generatedBy = "gpt-4o";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "root_causes", columnDefinition = "jsonb", nullable = false)
    private List<RootCause> rootCauses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contributing_factors", columnDefinition = "jsonb", nullable = false)
    private List<String> contributingFactors;

    @Column(name = "timeline_summary", columnDefinition = "TEXT")
    private String timelineSummary;

    @Column(name = "impact_summary", columnDefinition = "TEXT")
    private String impactSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recovery_actions", columnDefinition = "jsonb", nullable = false)
    private List<RecoveryAction> recoveryActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prevention_recs", columnDefinition = "jsonb", nullable = false)
    private List<String> preventionRecs;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "prompt_tokens")
    @Builder.Default
    private int promptTokens = 0;

    @Column(name = "completion_tokens")
    @Builder.Default
    private int completionTokens = 0;

    @Column(name = "generation_ms")
    @Builder.Default
    private long generationMs = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootCause {
        private String category;       // DEPLOYMENT, CODE_BUG, INFRA, DEPENDENCY, CONFIG
        private String description;
        private String evidence;
        private double confidence;     // 0.0 - 1.0
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecoveryAction {
        private int priority;          // 1 = highest
        private String action;
        private String rationale;
        private String owner;          // SRE, Developer, DevOps
        private String estimatedTime;
    }
}
