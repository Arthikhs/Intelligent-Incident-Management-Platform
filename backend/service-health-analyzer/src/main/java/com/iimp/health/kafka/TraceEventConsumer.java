package com.iimp.health.kafka;

import com.iimp.common.event.TraceEvent;
import com.iimp.health.service.ServiceHealthAnalyzerService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceEventConsumer {

    private final ServiceHealthAnalyzerService healthAnalyzerService;
    private final MeterRegistry meterRegistry;

    // Rolling counters per service — reset on each health computation
    private final Map<String, AtomicInteger> totalCounts  = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> errorCounts  = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong>    totalLatency = new ConcurrentHashMap<>();

    @KafkaListener(topics = "traces-topic", groupId = "service-health-analyzer",
                   concurrency = "3", containerFactory = "kafkaListenerContainerFactory")
    public void onTrace(TraceEvent event) {
        String svc = event.getServiceName();
        totalCounts .computeIfAbsent(svc, k -> new AtomicInteger()).incrementAndGet();
        totalLatency.computeIfAbsent(svc, k -> new AtomicLong()).addAndGet(event.getDurationMs());

        if ("ERROR".equals(event.getStatus()) || "TIMEOUT".equals(event.getStatus())) {
            errorCounts.computeIfAbsent(svc, k -> new AtomicInteger()).incrementAndGet();
        }

        int total  = totalCounts.get(svc).get();
        int errors = errorCounts.getOrDefault(svc, new AtomicInteger()).get();
        long latencySum = totalLatency.get(svc).get();

        // Compute health every 50 traces per service
        if (total % 50 == 0) {
            double errorRate    = total > 0 ? (double) errors / total : 0.0;
            double avgLatencyMs = total > 0 ? (double) latencySum / total : 0.0;
            double p99Estimate  = avgLatencyMs * 1.5; // rough p99 estimate from avg

            healthAnalyzerService.computeAndPersistHealth(
                svc, errorRate, p99Estimate, 99.9, 40.0, 50.0,
                total / 60.0, 1
            );

            log.info("Health computed for service={}, errorRate={}, avgLatency={}ms", svc, errorRate, avgLatencyMs);
            // reset counters
            totalCounts.put(svc, new AtomicInteger());
            errorCounts.put(svc, new AtomicInteger());
            totalLatency.put(svc, new AtomicLong());
        }
    }
}
