package com.iimp.slo.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.iimp.slo.service.SloService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentDowntimeConsumer {

    private final SloService sloService;

    @KafkaListener(topics = "incidents-topic", groupId = "slo-group")
    public void onIncident(JsonNode event) {
        try {
            String eventType = event.path("eventType").asText("");
            if (!"INCIDENT_RESOLVED".equals(eventType)) return;

            String service = event.path("affectedService").asText();
            double downtimeMinutes = event.path("durationMinutes").asDouble(0);
            if (service.isBlank() || downtimeMinutes == 0) return;

            sloService.recordDowntime(service, downtimeMinutes);
        } catch (Exception e) {
            log.error("Failed to process incident for SLO tracking", e);
        }
    }
}
