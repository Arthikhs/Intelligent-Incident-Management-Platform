package com.iimp.health.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_health")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "health_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal healthScore;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "HEALTHY";

    @Column(name = "error_rate", precision = 8, scale = 4)
    private BigDecimal errorRate;

    @Column(name = "p99_latency_ms", precision = 10, scale = 2)
    private BigDecimal p99LatencyMs;

    @Column(name = "availability_pct", precision = 5, scale = 2)
    private BigDecimal availabilityPct;

    @Column(name = "cpu_usage_pct", precision = 5, scale = 2)
    private BigDecimal cpuUsagePct;

    @Column(name = "memory_usage_pct", precision = 5, scale = 2)
    private BigDecimal memoryUsagePct;

    @Column(name = "request_rate_rps", precision = 10, scale = 2)
    private BigDecimal requestRateRps;

    @Column(name = "active_instances")
    @Builder.Default
    private int activeInstances = 1;

    @Column(name = "evaluated_at", nullable = false)
    @Builder.Default
    private Instant evaluatedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
