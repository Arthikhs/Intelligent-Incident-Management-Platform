package com.iimp.anomaly.service;

import com.iimp.anomaly.domain.AnomalyEvent;
import com.iimp.anomaly.domain.AnomalyEvent.AnomalyType;
import com.iimp.anomaly.domain.AnomalyEvent.AnomalySeverity;
import com.iimp.anomaly.repository.AnomalyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final double EWMA_ALPHA = 0.3;
    private static final int BASELINE_WINDOW = 60; // data points

    private final AnomalyEventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public Optional<AnomalyEvent> detectErrorRateAnomaly(String serviceName, double errorRate) {
        String key = "baseline:error:" + serviceName;
        List<Double> baseline = getBaseline(key);

        if (baseline.size() < 10) {
            updateBaseline(key, errorRate);
            return Optional.empty();
        }

        double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = computeStdDev(baseline, mean);
        double zScore = stdDev == 0 ? 0 : (errorRate - mean) / stdDev;
        double ewma = computeEwma(baseline, errorRate);

        updateBaseline(key, errorRate);

        if (Math.abs(zScore) > Z_SCORE_THRESHOLD) {
            AnomalyEvent event = buildEvent(serviceName, AnomalyType.ERROR_SPIKE,
                "error_rate", errorRate, mean, zScore, ewma);
            repository.save(event);
            publishAnomaly(event);
            log.warn("Anomaly detected: service={} metric=error_rate z={}", serviceName, zScore);
            return Optional.of(event);
        }
        return Optional.empty();
    }

    public Optional<AnomalyEvent> detectLatencyAnomaly(String serviceName, double p99LatencyMs) {
        String key = "baseline:latency:" + serviceName;
        List<Double> baseline = getBaseline(key);

        if (baseline.size() < 10) {
            updateBaseline(key, p99LatencyMs);
            return Optional.empty();
        }

        double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = computeStdDev(baseline, mean);
        double zScore = stdDev == 0 ? 0 : (p99LatencyMs - mean) / stdDev;
        double ewma = computeEwma(baseline, p99LatencyMs);

        updateBaseline(key, p99LatencyMs);

        if (zScore > Z_SCORE_THRESHOLD) {
            AnomalyEvent event = buildEvent(serviceName, AnomalyType.LATENCY_SPIKE,
                "p99_latency_ms", p99LatencyMs, mean, zScore, ewma);
            repository.save(event);
            publishAnomaly(event);
            return Optional.of(event);
        }
        return Optional.empty();
    }

    public List<AnomalyEvent> getRecentAnomalies(String serviceName) {
        return repository.findTop20ByServiceNameOrderByDetectedAtDesc(serviceName);
    }

    private AnomalyEvent buildEvent(String service, AnomalyType type, String metric,
                                     double observed, double expected, double zScore, double ewma) {
        AnomalySeverity severity = Math.abs(zScore) > 6 ? AnomalySeverity.CRITICAL
                                 : Math.abs(zScore) > 4 ? AnomalySeverity.HIGH
                                 : Math.abs(zScore) > 3 ? AnomalySeverity.MEDIUM
                                 : AnomalySeverity.LOW;

        return AnomalyEvent.builder()
            .serviceName(service)
            .anomalyType(type)
            .metricName(metric)
            .observedValue(observed)
            .expectedValue(expected)
            .zScore(zScore)
            .ewmaValue(ewma)
            .severity(severity)
            .description(String.format("%s anomaly in %s: observed=%.2f expected=%.2f z=%.2f",
                type, service, observed, expected, zScore))
            .acknowledged(false)
            .detectedAt(Instant.now())
            .build();
    }

    private double computeStdDev(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0);
        return Math.sqrt(variance);
    }

    private double computeEwma(List<Double> values, double current) {
        double ewma = values.get(0);
        for (double v : values) ewma = EWMA_ALPHA * v + (1 - EWMA_ALPHA) * ewma;
        return EWMA_ALPHA * current + (1 - EWMA_ALPHA) * ewma;
    }

    @SuppressWarnings("unchecked")
    private List<Double> getBaseline(String key) {
        List<Double> list = (List<Double>) redisTemplate.opsForValue().get(key);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private void updateBaseline(String key, double value) {
        List<Double> baseline = getBaseline(key);
        baseline.add(value);
        if (baseline.size() > BASELINE_WINDOW) baseline.remove(0);
        redisTemplate.opsForValue().set(key, baseline, Duration.ofHours(24));
    }

    private void publishAnomaly(AnomalyEvent event) {
        kafkaTemplate.send("anomaly-topic", event.getServiceName(), event);
    }
}
