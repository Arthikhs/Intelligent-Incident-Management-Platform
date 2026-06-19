package com.iimp.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String serviceName;
    private BigDecimal healthScore;
    private String status;           // HEALTHY, DEGRADED, CRITICAL, UNKNOWN
    private BigDecimal errorRate;
    private BigDecimal p50LatencyMs;
    private BigDecimal p95LatencyMs;
    private BigDecimal p99LatencyMs;
    private BigDecimal requestRateRps;
    private BigDecimal availabilityPct;
    private BigDecimal cpuUsagePct;
    private BigDecimal memoryUsagePct;
    private Integer activeInstances;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant evaluatedAt = Instant.now();
}
