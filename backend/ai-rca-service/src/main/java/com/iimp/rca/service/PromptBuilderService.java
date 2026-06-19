package com.iimp.rca.service;

import com.iimp.rca.dto.IncidentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Assembles the LLM prompt from incident context data.
 */
@Slf4j
@Service
public class PromptBuilderService {

    public String buildRcaPrompt(IncidentContext ctx, String template) {
        String errors = ctx.getRecentErrors() == null ? "No errors captured" :
            ctx.getRecentErrors().stream()
                .map(e -> "[%s] %s %s: %s".formatted(e.getLoggedAt(), e.getServiceName(), e.getLevel(), e.getMessage()))
                .collect(Collectors.joining("\n"));

        String deployments = ctx.getRecentDeployments() == null ? "No recent deployments" :
            ctx.getRecentDeployments().stream()
                .map(d -> "[%s] %s v%s (%s) by %s, risk=%.2f, sha=%s"
                    .formatted(d.getStartedAt(), d.getServiceName(), d.getVersion(),
                               d.getStatus(), d.getDeployedBy(), d.getRiskScore() != null ? d.getRiskScore() : 0.0, d.getCommitSha()))
                .collect(Collectors.joining("\n"));

        String alerts = ctx.getFiringAlerts() == null ? "No firing alerts" :
            ctx.getFiringAlerts().stream()
                .map(a -> "[%s] %s - %s (%s): %s".formatted(a.getFiredAt(), a.getServiceName(), a.getAlertName(), a.getSeverity(), a.getMessage()))
                .collect(Collectors.joining("\n"));

        String traces = ctx.getErrorTraces() == null ? "No error traces" :
            ctx.getErrorTraces().stream()
                .map(t -> "[%s] %s.%s (%s) durationMs=%d error=%s"
                    .formatted(t.getSpanStartedAt(), t.getServiceName(), t.getOperationName(),
                               t.getStatus(), t.getDurationMs() != null ? t.getDurationMs() : 0, t.getErrorMessage()))
                .collect(Collectors.joining("\n"));

        String health = ctx.getServiceHealth() == null ? "Health data unavailable" :
            "Service=%s healthScore=%.1f errorRate=%.4f p99Latency=%.1fms availability=%.2f%%"
                .formatted(
                    ctx.getServiceHealth().getServiceName(),
                    ctx.getServiceHealth().getHealthScore() != null ? ctx.getServiceHealth().getHealthScore() : 0.0,
                    ctx.getServiceHealth().getErrorRate() != null ? ctx.getServiceHealth().getErrorRate() : 0.0,
                    ctx.getServiceHealth().getP99LatencyMs() != null ? ctx.getServiceHealth().getP99LatencyMs() : 0.0,
                    ctx.getServiceHealth().getAvailabilityPct() != null ? ctx.getServiceHealth().getAvailabilityPct() : 0.0
                );

        return template
            .replace("{incidentId}", ctx.getIncidentId().toString())
            .replace("{title}", ctx.getIncidentTitle())
            .replace("{severity}", ctx.getSeverity())
            .replace("{affectedServices}", String.join(", ", ctx.getAffectedServices()))
            .replace("{detectedAt}", ctx.getDetectedAt().toString())
            .replace("{recentErrors}", errors)
            .replace("{recentDeployments}", deployments)
            .replace("{firingAlerts}", alerts)
            .replace("{errorTraces}", traces)
            .replace("{serviceHealth}", health);
    }
}
