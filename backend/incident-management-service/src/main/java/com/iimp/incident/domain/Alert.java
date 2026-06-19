package com.iimp.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "alert_name", nullable = false, length = 500)
    private String alertName;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "FIRING";

    @Column(length = 100)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> labels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> annotations;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "fired_at", nullable = false)
    @Builder.Default
    private Instant firedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
