package com.iimp.health.service;

import com.iimp.common.event.ServiceHealthEvent;
import com.iimp.health.domain.ServiceHealth;
import com.iimp.health.repository.ServiceHealthRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Computes composite health scores for each service using:
 * - Error rate (30% weight)
 * - P99 latency (25% weight)
 * - Availability (30% weight)
 * - Resource usage (15% weight)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceHealthAnalyzerService {

    private static final double ERROR_RATE_WEIGHT   = 0.30;
    private static final double LATENCY_WEIGHT      = 0.25;
    private static final double AVAILABILITY_WEIGHT = 0.30;
    private static final double RESOURCE_WEIGHT     = 0.15;

    // SLO thresholds
    private static final double MAX_ACCEPTABLE_ERROR_RATE    = 0.05;  // 5%
    private static final double MAX_ACCEPTABLE_P99_LATENCY   = 1000.0; // 1s
    private static final double MIN_ACCEPTABLE_AVAILABILITY  = 99.0;   // 99%
    private static final double MAX_ACCEPTABLE_CPU           = 80.0;   // 80%

    private final ServiceHealthRepository healthRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Transactional
    public ServiceHealth computeAndPersistHealth(
        String serviceName,
        double errorRate,
        double p99LatencyMs,
        double availabilityPct,
        double cpuUsagePct,
        double memoryUsagePct,
        double requestRateRps,
        int activeInstances
    ) {
        double score = computeHealthScore(errorRate, p99LatencyMs, availabilityPct, cpuUsagePct);
        String status = deriveStatus(score);

        ServiceHealth health = ServiceHealth.builder()
            .serviceName(serviceName)
            .healthScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
            .status(status)
            .errorRate(BigDecimal.valueOf(errorRate))
            .p99LatencyMs(BigDecimal.valueOf(p99LatencyMs))
            .availabilityPct(BigDecimal.valueOf(availabilityPct))
            .cpuUsagePct(BigDecimal.valueOf(cpuUsagePct))
            .memoryUsagePct(BigDecimal.valueOf(memoryUsagePct))
            .requestRateRps(BigDecimal.valueOf(requestRateRps))
            .activeInstances(activeInstances)
            .evaluatedAt(Instant.now())
            .build();

        health = healthRepository.save(health);

        // Emit to Kafka for other consumers
        ServiceHealthEvent event = ServiceHealthEvent.builder()
            .serviceName(serviceName)
            .healthScore(health.getHealthScore())
            .status(status)
            .errorRate(health.getErrorRate())
            .p99LatencyMs(health.getP99LatencyMs())
            .availabilityPct(health.getAvailabilityPct())
            .build();
        kafkaTemplate.send("service-health-topic", serviceName, event);

        // Update Prometheus metrics
        meterRegistry.gauge("service.health.score", List.of(io.micrometer.core.instrument.Tag.of("service", serviceName)), score);
        meterRegistry.gauge("service.error.rate", List.of(io.micrometer.core.instrument.Tag.of("service", serviceName)), errorRate);

        if ("CRITICAL".equals(status) || "DEGRADED".equals(status)) {
            log.warn("Service health degraded: service={}, score={}, status={}", serviceName, score, status);
        }

        return health;
    }

    private double computeHealthScore(double errorRate, double p99LatencyMs,
                                      double availabilityPct, double cpuUsagePct) {
        // Normalize each dimension to 0-100 scale
        double errorScore       = Math.max(0, 100 - (errorRate / MAX_ACCEPTABLE_ERROR_RATE) * 100);
        double latencyScore     = Math.max(0, 100 - (p99LatencyMs / MAX_ACCEPTABLE_P99_LATENCY) * 100);
        double availScore       = Math.max(0, (availabilityPct / MIN_ACCEPTABLE_AVAILABILITY) * 100);
        double resourceScore    = Math.max(0, 100 - (cpuUsagePct / MAX_ACCEPTABLE_CPU) * 100);

        return (errorScore * ERROR_RATE_WEIGHT)
             + (latencyScore * LATENCY_WEIGHT)
             + (availScore * AVAILABILITY_WEIGHT)
             + (resourceScore * RESOURCE_WEIGHT);
    }

    private String deriveStatus(double score) {
        if (score >= 85) return "HEALTHY";
        if (score >= 60) return "DEGRADED";
        return "CRITICAL";
    }

    @Transactional(readOnly = true)
    public List<ServiceHealth> getLatestForAllServices() {
        return healthRepository.findLatestForAllServices();
    }

    @Transactional(readOnly = true)
    public ServiceHealth getLatestForService(String serviceName) {
        return healthRepository.findTopByServiceNameOrderByEvaluatedAtDesc(serviceName)
            .orElseThrow(() -> new RuntimeException("No health data for service: " + serviceName));
    }
}
