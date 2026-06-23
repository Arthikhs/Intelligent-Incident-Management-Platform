package com.iimp.slo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slos")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Slo {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String metricName; // availability, latency_p99, error_rate

    @Column(nullable = false)
    private Double targetPercent; // e.g. 99.9

    @Column(nullable = false)
    private Double errorBudgetMinutes; // total allowed downtime per window

    @Column(nullable = false)
    private Double errorBudgetRemainingMinutes;

    @Column(nullable = false)
    private Double currentCompliancePercent;

    @Column(nullable = false)
    private String windowDays; // e.g. "30"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SloStatus status;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum SloStatus { OK, WARNING, BREACHED }
}
