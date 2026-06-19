package com.iimp.correlation.service;

import com.iimp.common.event.AlertEvent;
import com.iimp.common.event.IncidentEvent;
import com.iimp.correlation.model.AlertCluster;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Correlates incoming alerts using:
 * 1. Fingerprint deduplication (same alert, same service)
 * 2. Service-based clustering (multiple alerts from same service → 1 incident)
 * 3. Time-window grouping (alerts within 5-minute window)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCorrelationService {

    private static final Duration CORRELATION_WINDOW = Duration.ofMinutes(5);
    private static final int CLUSTER_THRESHOLD = 3;  // alerts needed to create incident
    private static final String CLUSTER_CACHE_PREFIX = "alert:cluster:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    // In-memory cluster map (also backed by Redis for distributed scenario)
    private final Map<String, AlertCluster> activeClusters = new ConcurrentHashMap<>();

    public void processAlert(AlertEvent alert) {
        // Step 1: Deduplication by fingerprint
        if (isDuplicate(alert)) {
            log.debug("Duplicate alert suppressed: fingerprint={}", alert.getFingerprint());
            meterRegistry.counter("alerts.deduplicated").increment();
            return;
        }

        // Step 2: Find or create cluster
        String clusterKey = buildClusterKey(alert);
        AlertCluster cluster = activeClusters.computeIfAbsent(clusterKey, k ->
            AlertCluster.builder()
                .clusterId(UUID.randomUUID().toString())
                .serviceName(alert.getServiceName())
                .severity(alert.getSeverity())
                .alerts(new ArrayList<>())
                .windowStart(Instant.now())
                .build()
        );

        // Step 3: Add alert if within window
        if (Duration.between(cluster.getWindowStart(), Instant.now()).compareTo(CORRELATION_WINDOW) <= 0) {
            cluster.getAlerts().add(alert);
            log.info("Alert added to cluster: clusterId={}, alertCount={}", cluster.getClusterId(), cluster.getAlerts().size());
        } else {
            // Window expired, start new cluster
            activeClusters.remove(clusterKey);
            cluster = AlertCluster.builder()
                .clusterId(UUID.randomUUID().toString())
                .serviceName(alert.getServiceName())
                .severity(alert.getSeverity())
                .alerts(new ArrayList<>(List.of(alert)))
                .windowStart(Instant.now())
                .build();
            activeClusters.put(clusterKey, cluster);
        }

        // Step 4: If cluster reaches threshold, emit incident
        if (cluster.getAlerts().size() >= CLUSTER_THRESHOLD && !cluster.isIncidentCreated()) {
            cluster.setIncidentCreated(true);
            emitIncidentEvent(cluster);
        }

        meterRegistry.gauge("alerts.cluster.size", cluster.getAlerts().size());
    }

    private boolean isDuplicate(AlertEvent alert) {
        String key = "dedup:" + alert.getFingerprint();
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) return true;
        redisTemplate.opsForValue().set(key, "1", CORRELATION_WINDOW);
        return false;
    }

    private String buildClusterKey(AlertEvent alert) {
        return alert.getServiceName() + ":" + normalizeSeverity(alert.getSeverity());
    }

    private String normalizeSeverity(String severity) {
        return (severity != null) ? severity.toUpperCase() : "UNKNOWN";
    }

    private void emitIncidentEvent(AlertCluster cluster) {
        List<String> services = cluster.getAlerts().stream()
            .map(AlertEvent::getServiceName)
            .distinct().toList();

        IncidentEvent incidentEvent = IncidentEvent.builder()
            .incidentId(cluster.getClusterId())
            .title("Alert cluster: " + cluster.getAlerts().size() + " alerts on " + cluster.getServiceName())
            .severity(cluster.getSeverity())
            .status("OPEN")
            .affectedServices(services)
            .eventType("CREATED")
            .build();

        kafkaTemplate.send("incidents-topic", cluster.getClusterId(), incidentEvent);
        meterRegistry.counter("incidents.auto_created", "source", "alert_correlation").increment();
        log.info("Incident emitted from alert cluster: clusterId={}, service={}, severity={}",
            cluster.getClusterId(), cluster.getServiceName(), cluster.getSeverity());
    }
}
