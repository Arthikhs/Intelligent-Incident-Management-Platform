package com.iimp.logs.service;

import com.iimp.common.event.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Simple rule-based anomaly detection.
 * In production, replace with an ML model (e.g., Isolation Forest via ONNX or external ML service).
 */
@Slf4j
@Service
public class AnomalyDetectionService {

    private static final Set<String> CRITICAL_PATTERNS = Set.of(
        "OutOfMemoryError", "StackOverflowError", "killed", "OOM",
        "SIGSEGV", "core dump", "disk full", "no space left"
    );

    private static final Set<String> HIGH_PATTERNS = Set.of(
        "Connection refused", "Connection timed out", "too many connections",
        "deadlock", "rollback", "circuit breaker", "fallback"
    );

    /**
     * Returns an anomaly score between 0.0 (normal) and 1.0 (highly anomalous).
     */
    public double score(LogEvent event) {
        if (event.getMessage() == null) return 0.0;

        String msg = event.getMessage().toLowerCase();
        String level = event.getLevel();

        // FATAL = always anomaly
        if ("FATAL".equals(level)) return 1.0;

        // Critical pattern match
        for (String pattern : CRITICAL_PATTERNS) {
            if (msg.contains(pattern.toLowerCase())) return 0.95;
        }

        // Has exception + ERROR = high anomaly
        if ("ERROR".equals(level) && event.getExceptionClass() != null) return 0.85;

        // High pattern match
        for (String pattern : HIGH_PATTERNS) {
            if (msg.contains(pattern.toLowerCase())) return 0.80;
        }

        // ERROR without exception
        if ("ERROR".equals(level)) return 0.60;

        // WARN = low anomaly
        if ("WARN".equals(level)) return 0.30;

        return 0.0;
    }
}
