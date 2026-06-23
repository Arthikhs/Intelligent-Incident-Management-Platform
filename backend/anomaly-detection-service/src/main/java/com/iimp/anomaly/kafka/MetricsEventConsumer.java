package com.iimp.anomaly.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.iimp.anomaly.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsEventConsumer {

    private final AnomalyDetectionService anomalyDetectionService;

    @KafkaListener(topics = "metrics-topic", groupId = "anomaly-detection-group")
    public void onMetrics(JsonNode event) {
        try {
            String service = event.path("serviceName").asText();
            double errorRate = event.path("errorRate").asDouble(0);
            double p99 = event.path("p99LatencyMs").asDouble(0);

            if (errorRate > 0) anomalyDetectionService.detectErrorRateAnomaly(service, errorRate);
            if (p99 > 0)       anomalyDetectionService.detectLatencyAnomaly(service, p99);
        } catch (Exception e) {
            log.error("Failed to process metrics event", e);
        }
    }
}
