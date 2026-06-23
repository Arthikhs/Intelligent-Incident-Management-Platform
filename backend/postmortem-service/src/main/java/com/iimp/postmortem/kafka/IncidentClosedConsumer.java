package com.iimp.postmortem.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.iimp.postmortem.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentClosedConsumer {

    private final PostmortemService postmortemService;

    @KafkaListener(topics = "incidents-topic", groupId = "postmortem-group")
    public void onIncident(JsonNode event) {
        try {
            if (!"INCIDENT_CLOSED".equals(event.path("eventType").asText())) return;

            UUID incidentId = UUID.fromString(event.path("incidentId").asText());
            String title     = event.path("title").asText("Unknown Incident");
            String severity  = event.path("severity").asText("UNKNOWN");
            String rca       = event.path("rcaSummary").asText("No RCA available.");
            String timeline  = event.path("timeline").toString();

            postmortemService.generate(incidentId, title, rca, timeline, severity);
        } catch (Exception e) {
            log.error("Failed to generate postmortem from incident event", e);
        }
    }
}
