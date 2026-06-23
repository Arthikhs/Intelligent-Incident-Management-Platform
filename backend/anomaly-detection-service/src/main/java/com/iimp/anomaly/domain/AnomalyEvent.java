package com.iimp.anomaly.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anomaly_events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnomalyEvent {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnomalyType anomalyType;

    @Column(nullable = false)
    private String metricName;

    private Double observedValue;
    private Double expectedValue;
    private Double zScore;
    private Double ewmaValue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnomalySeverity severity;

    private String description;
    private boolean acknowledged;

    @Column(nullable = false)
    private Instant detectedAt;

    public enum AnomalyType {
        ERROR_SPIKE, LATENCY_SPIKE, TRAFFIC_SPIKE, TRAFFIC_DROP, RESOURCE_SATURATION, MEMORY_LEAK
    }

    public enum AnomalySeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}
