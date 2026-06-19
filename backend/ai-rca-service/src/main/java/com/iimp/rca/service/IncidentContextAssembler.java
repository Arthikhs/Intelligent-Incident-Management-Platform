package com.iimp.rca.service;

import com.iimp.common.event.IncidentEvent;
import com.iimp.rca.dto.IncidentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Assembles full incident context from multiple data sources for AI analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentContextAssembler {

    private final JdbcTemplate jdbcTemplate;

    public IncidentContext assemble(IncidentEvent incidentEvent) {
        UUID incidentId = UUID.fromString(incidentEvent.getIncidentId());
        Instant window = Instant.now().minus(2, ChronoUnit.HOURS);

        List<IncidentContext.LogSummary> errors = fetchRecentErrors(incidentEvent.getAffectedServices(), window);
        List<IncidentContext.DeploymentSummary> deployments = fetchRecentDeployments(incidentEvent.getAffectedServices(), window);
        List<IncidentContext.AlertSummary> alerts = fetchFiringAlerts(incidentEvent.getAffectedServices());
        List<IncidentContext.TraceSummary> traces = fetchErrorTraces(incidentEvent.getAffectedServices(), window);
        IncidentContext.ServiceHealthSummary health = fetchServiceHealth(incidentEvent.getAffectedServices());

        return IncidentContext.builder()
            .incidentId(incidentId)
            .incidentTitle(incidentEvent.getTitle())
            .severity(incidentEvent.getSeverity())
            .status(incidentEvent.getStatus())
            .affectedServices(incidentEvent.getAffectedServices())
            .detectedAt(incidentEvent.getOccurredAt())
            .recentErrors(errors)
            .recentDeployments(deployments)
            .firingAlerts(alerts)
            .errorTraces(traces)
            .serviceHealth(health)
            .build();
    }

    private List<IncidentContext.LogSummary> fetchRecentErrors(List<String> services, Instant since) {
        if (services == null || services.isEmpty()) return List.of();
        try {
            String placeholders = String.join(",", services.stream().map(s -> "?").toList());
            String sql = """
                SELECT service_name, level, message, exception_class, logged_at
                FROM logs
                WHERE service_name = ANY(?) AND level IN ('ERROR','FATAL')
                  AND logged_at >= ?
                ORDER BY logged_at DESC
                LIMIT 50
                """;
            return jdbcTemplate.query(sql,
                ps -> {
                    ps.setArray(1, ps.getConnection().createArrayOf("text", services.toArray()));
                    ps.setObject(2, since);
                },
                (rs, i) -> IncidentContext.LogSummary.builder()
                    .serviceName(rs.getString("service_name"))
                    .level(rs.getString("level"))
                    .message(rs.getString("message"))
                    .exceptionClass(rs.getString("exception_class"))
                    .loggedAt(rs.getTimestamp("logged_at").toInstant())
                    .build()
            );
        } catch (Exception e) {
            log.warn("Failed to fetch recent errors: {}", e.getMessage());
            return List.of();
        }
    }

    private List<IncidentContext.DeploymentSummary> fetchRecentDeployments(List<String> services, Instant since) {
        if (services == null || services.isEmpty()) return List.of();
        try {
            String sql = """
                SELECT service_name, version, status, deployed_by, commit_sha, risk_score, started_at
                FROM deployments
                WHERE service_name = ANY(?) AND started_at >= ?
                ORDER BY started_at DESC
                LIMIT 10
                """;
            return jdbcTemplate.query(sql,
                ps -> {
                    ps.setArray(1, ps.getConnection().createArrayOf("text", services.toArray()));
                    ps.setObject(2, since);
                },
                (rs, i) -> IncidentContext.DeploymentSummary.builder()
                    .serviceName(rs.getString("service_name"))
                    .version(rs.getString("version"))
                    .status(rs.getString("status"))
                    .deployedBy(rs.getString("deployed_by"))
                    .commitSha(rs.getString("commit_sha"))
                    .riskScore(rs.getDouble("risk_score"))
                    .startedAt(rs.getTimestamp("started_at").toInstant())
                    .build()
            );
        } catch (Exception e) {
            log.warn("Failed to fetch deployments: {}", e.getMessage());
            return List.of();
        }
    }

    private List<IncidentContext.AlertSummary> fetchFiringAlerts(List<String> services) {
        if (services == null || services.isEmpty()) return List.of();
        try {
            String sql = """
                SELECT service_name, alert_name, severity, message, fired_at
                FROM alerts
                WHERE service_name = ANY(?) AND status = 'FIRING'
                ORDER BY fired_at DESC
                LIMIT 20
                """;
            return jdbcTemplate.query(sql,
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("text", services.toArray())),
                (rs, i) -> IncidentContext.AlertSummary.builder()
                    .serviceName(rs.getString("service_name"))
                    .alertName(rs.getString("alert_name"))
                    .severity(rs.getString("severity"))
                    .message(rs.getString("message"))
                    .firedAt(rs.getTimestamp("fired_at").toInstant())
                    .build()
            );
        } catch (Exception e) {
            log.warn("Failed to fetch alerts: {}", e.getMessage());
            return List.of();
        }
    }

    private List<IncidentContext.TraceSummary> fetchErrorTraces(List<String> services, Instant since) {
        if (services == null || services.isEmpty()) return List.of();
        try {
            String sql = """
                SELECT service_name, operation_name, status, duration_ms, error_message, span_started_at
                FROM traces
                WHERE service_name = ANY(?) AND status IN ('ERROR','TIMEOUT')
                  AND span_started_at >= ?
                ORDER BY span_started_at DESC
                LIMIT 20
                """;
            return jdbcTemplate.query(sql,
                ps -> {
                    ps.setArray(1, ps.getConnection().createArrayOf("text", services.toArray()));
                    ps.setObject(2, since);
                },
                (rs, i) -> IncidentContext.TraceSummary.builder()
                    .serviceName(rs.getString("service_name"))
                    .operationName(rs.getString("operation_name"))
                    .status(rs.getString("status"))
                    .durationMs(rs.getLong("duration_ms"))
                    .errorMessage(rs.getString("error_message"))
                    .spanStartedAt(rs.getTimestamp("span_started_at").toInstant())
                    .build()
            );
        } catch (Exception e) {
            log.warn("Failed to fetch traces: {}", e.getMessage());
            return List.of();
        }
    }

    private IncidentContext.ServiceHealthSummary fetchServiceHealth(List<String> services) {
        if (services == null || services.isEmpty()) return null;
        try {
            String primaryService = services.get(0);
            String sql = """
                SELECT service_name, health_score, error_rate, p99_latency_ms, availability_pct
                FROM service_health
                WHERE service_name = ?
                ORDER BY evaluated_at DESC
                LIMIT 1
                """;
            return jdbcTemplate.queryForObject(sql, new Object[]{primaryService},
                (rs, i) -> IncidentContext.ServiceHealthSummary.builder()
                    .serviceName(rs.getString("service_name"))
                    .healthScore(rs.getDouble("health_score"))
                    .errorRate(rs.getDouble("error_rate"))
                    .p99LatencyMs(rs.getDouble("p99_latency_ms"))
                    .availabilityPct(rs.getDouble("availability_pct"))
                    .build()
            );
        } catch (Exception e) {
            log.warn("Failed to fetch service health: {}", e.getMessage());
            return null;
        }
    }
}
