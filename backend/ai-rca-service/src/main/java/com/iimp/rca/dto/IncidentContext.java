package com.iimp.rca.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Complete incident context assembled for AI analysis.
 */
@Data
@Builder
public class IncidentContext {

    private UUID incidentId;
    private String incidentTitle;
    private String severity;
    private String status;
    private List<String> affectedServices;
    private Instant detectedAt;

    private List<LogSummary> recentErrors;
    private List<DeploymentSummary> recentDeployments;
    private List<AlertSummary> firingAlerts;
    private List<TraceSummary> errorTraces;
    private ServiceHealthSummary serviceHealth;

    @Data
    @Builder
    public static class LogSummary {
        private String serviceName;
        private String level;
        private String message;
        private String exceptionClass;
        private Instant loggedAt;
    }

    @Data
    @Builder
    public static class DeploymentSummary {
        private String serviceName;
        private String version;
        private String status;
        private String deployedBy;
        private String commitSha;
        private Double riskScore;
        private Instant startedAt;
    }

    @Data
    @Builder
    public static class AlertSummary {
        private String serviceName;
        private String alertName;
        private String severity;
        private String message;
        private Instant firedAt;
    }

    @Data
    @Builder
    public static class TraceSummary {
        private String serviceName;
        private String operationName;
        private String status;
        private Long durationMs;
        private String errorMessage;
        private Instant spanStartedAt;
    }

    @Data
    @Builder
    public static class ServiceHealthSummary {
        private String serviceName;
        private Double healthScore;
        private Double errorRate;
        private Double p99LatencyMs;
        private Double availabilityPct;
    }
}
