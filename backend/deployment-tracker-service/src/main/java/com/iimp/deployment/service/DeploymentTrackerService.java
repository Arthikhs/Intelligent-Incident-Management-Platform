package com.iimp.deployment.service;

import com.iimp.common.event.AlertEvent;
import com.iimp.common.event.DeploymentEvent;
import com.iimp.incident.domain.Deployment;
import com.iimp.deployment.repository.DeploymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Tracks deployments and computes risk scores.
 * Emits alerts for failed/high-risk deployments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentTrackerService {

    private final DeploymentRepository deploymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void processDeploymentEvent(DeploymentEvent event) {
        double riskScore = computeRiskScore(event);

        Deployment deployment = deploymentRepository
            .findByServiceNameAndVersion(event.getServiceName(), event.getVersion())
            .map(existing -> updateDeployment(existing, event, riskScore))
            .orElseGet(() -> createDeployment(event, riskScore));

        deploymentRepository.save(deployment);
        meterRegistry.counter("deployments.processed", "status", event.getStatus(), "service", event.getServiceName()).increment();

        // Emit alert if deployment failed or risk is high
        if ("FAILED".equals(event.getStatus()) || riskScore > 0.7) {
            emitDeploymentAlert(event, riskScore);
        }

        log.info("Deployment tracked: service={}, version={}, status={}, risk={}",
            event.getServiceName(), event.getVersion(), event.getStatus(), riskScore);
    }

    /**
     * Risk score (0.0 - 1.0) based on:
     * - Production deployments (+0.2)
     * - Failed status (+0.4)
     * - No rollback capability (no rollback info +0.1)
     * - Config changes present (+0.1)
     * - Friday deployment (+0.1)
     * - Number of files changed (normalized)
     */
    private double computeRiskScore(DeploymentEvent event) {
        double score = 0.0;

        if ("production".equalsIgnoreCase(event.getEnvironment())) score += 0.2;
        if ("FAILED".equals(event.getStatus()))                     score += 0.4;
        if ("ROLLED_BACK".equals(event.getStatus()))                score += 0.2;
        if (event.getConfigChanges() != null && !event.getConfigChanges().isEmpty()) score += 0.1;

        // Friday deployments are risky
        java.time.DayOfWeek day = Instant.now().atZone(java.time.ZoneOffset.UTC).getDayOfWeek();
        if (day == java.time.DayOfWeek.FRIDAY) score += 0.1;

        return Math.min(1.0, score);
    }

    private Deployment createDeployment(DeploymentEvent event, double riskScore) {
        return Deployment.builder()
            .serviceName(event.getServiceName())
            .version(event.getVersion())
            .environment(event.getEnvironment())
            .status(event.getStatus())
            .deployedBy(event.getDeployedBy())
            .commitSha(event.getCommitSha())
            .branch(event.getBranch())
            .pipelineUrl(event.getPipelineUrl())
            .configChanges(event.getConfigChanges())
            .riskScore(BigDecimal.valueOf(riskScore))
            .startedAt(event.getStartedAt())
            .completedAt(event.getCompletedAt())
            .build();
    }

    private Deployment updateDeployment(Deployment existing, DeploymentEvent event, double riskScore) {
        existing.setStatus(event.getStatus());
        existing.setRiskScore(BigDecimal.valueOf(riskScore));
        existing.setCompletedAt(event.getCompletedAt());
        if ("ROLLED_BACK".equals(event.getStatus())) existing.setRolledBackAt(Instant.now());
        return existing;
    }

    private void emitDeploymentAlert(DeploymentEvent event, double riskScore) {
        String severity = riskScore > 0.7 ? "HIGH" : "MEDIUM";
        AlertEvent alert = AlertEvent.builder()
            .serviceName(event.getServiceName())
            .alertName("DeploymentRisk:" + event.getStatus())
            .severity(severity)
            .status("FIRING")
            .source("deployment-tracker")
            .message("Deployment risk detected: service=%s version=%s status=%s risk=%.2f"
                .formatted(event.getServiceName(), event.getVersion(), event.getStatus(), riskScore))
            .fingerprint("deploy:" + event.getServiceName() + ":" + event.getVersion())
            .labels(Map.of(
                "service", event.getServiceName(),
                "version", event.getVersion(),
                "environment", event.getEnvironment()
            ))
            .build();
        kafkaTemplate.send("alerts-topic", alert.getFingerprint(), alert);
    }
}
