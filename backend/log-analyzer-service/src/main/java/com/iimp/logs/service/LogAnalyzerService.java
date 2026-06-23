package com.iimp.logs.service;

import com.iimp.common.event.AlertEvent;
import com.iimp.common.event.LogEvent;
import com.iimp.logs.domain.Log;
import com.iimp.logs.repository.LogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Analyzes incoming logs for:
 * - Error classification
 * - Anomaly scoring
 * - Alert generation for critical patterns
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalyzerService {

    private static final double ANOMALY_THRESHOLD = 0.75;
    private static final Map<String, String> EXCEPTION_SEVERITY_MAP = Map.of(
        "OutOfMemoryError",         "CRITICAL",
        "StackOverflowError",       "CRITICAL",
        "DatabaseException",        "HIGH",
        "ConnectionRefusedException","HIGH",
        "TimeoutException",         "MEDIUM",
        "NullPointerException",     "MEDIUM"
    );

    private final LogRepository logRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AnomalyDetectionService anomalyDetector;
    private final MeterRegistry meterRegistry;

    public void processLog(LogEvent event) {
        double anomalyScore = anomalyDetector.score(event);
        boolean isAnomaly = anomalyScore >= ANOMALY_THRESHOLD;

        Log logEntity = Log.builder()
            .serviceName(event.getServiceName())
            .level(event.getLevel())
            .message(event.getMessage())
            .loggerName(event.getLoggerName())
            .threadName(event.getThreadName())
            .traceId(event.getTraceId())
            .spanId(event.getSpanId())
            .exceptionClass(event.getExceptionClass())
            .stackTrace(event.getStackTrace())
            .anomalyScore(anomalyScore)
            .isAnomaly(isAnomaly)
            .loggedAt(event.getLoggedAt())
            .build();

        logRepository.save(logEntity);

        meterRegistry.counter("logs.processed", "level", event.getLevel(),
            "service", event.getServiceName()).increment();

        if (isAnomaly || isErrorOrFatal(event.getLevel())) {
            emitAlertFromLog(event, anomalyScore);
        }
    }

    private void emitAlertFromLog(LogEvent event, double anomalyScore) {
        String severity = classifySeverity(event);
        String fingerprint = buildFingerprint(event);

        AlertEvent alert = AlertEvent.builder()
            .serviceName(event.getServiceName())
            .alertName("LogAnomaly:" + event.getLevel())
            .severity(severity)
            .status("FIRING")
            .source("log-analyzer")
            .message(truncate(event.getMessage(), 500))
            .fingerprint(fingerprint)
            .labels(Map.of(
                "service", event.getServiceName(),
                "level", event.getLevel(),
                "anomaly_score", String.valueOf(anomalyScore)
            ))
            .build();

        kafkaTemplate.send("alerts-topic", fingerprint, alert);
        log.info("Alert emitted from log: service={}, level={}, anomalyScore={}",
            event.getServiceName(), event.getLevel(), anomalyScore);
    }

    private String classifySeverity(LogEvent event) {
        if ("FATAL".equals(event.getLevel())) return "CRITICAL";
        if (event.getExceptionClass() != null) {
            for (Map.Entry<String, String> entry : EXCEPTION_SEVERITY_MAP.entrySet()) {
                if (event.getExceptionClass().contains(entry.getKey())) return entry.getValue();
            }
        }
        return "ERROR".equals(event.getLevel()) ? "HIGH" : "MEDIUM";
    }

    private boolean isErrorOrFatal(String level) {
        return "ERROR".equals(level) || "FATAL".equals(level);
    }

    private String buildFingerprint(LogEvent event) {
        String base = event.getServiceName() + ":" + event.getLevel() + ":" +
            (event.getExceptionClass() != null ? event.getExceptionClass() : event.getMessage().substring(0, Math.min(50, event.getMessage().length())));
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
