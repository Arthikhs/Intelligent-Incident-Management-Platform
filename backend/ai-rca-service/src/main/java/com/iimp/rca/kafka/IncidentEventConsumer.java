package com.iimp.rca.kafka;

import com.iimp.common.event.IncidentEvent;
import com.iimp.rca.service.AiRcaService;
import com.iimp.rca.service.IncidentContextAssembler;
import com.iimp.rca.dto.IncidentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventConsumer {

    private final AiRcaService aiRcaService;
    private final IncidentContextAssembler contextAssembler;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "incidents-topic", groupId = "ai-rca-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onIncidentEvent(IncidentEvent event) {
        // Only trigger RCA for newly OPEN incidents with HIGH/CRITICAL severity
        if (!"CREATED".equals(event.getEventType())) return;
        if (!isSevereEnough(event.getSeverity())) return;

        log.info("Triggering AI RCA for incident: id={}, severity={}", event.getIncidentId(), event.getSeverity());

        try {
            IncidentContext context = contextAssembler.assemble(event);
            var report = aiRcaService.generateRca(context);
            kafkaTemplate.send("rca-topic", event.getIncidentId(), report);
            log.info("RCA published to rca-topic: incidentId={}", event.getIncidentId());
        } catch (Exception e) {
            log.error("Failed to generate RCA for incident: {}", event.getIncidentId(), e);
        }
    }

    private boolean isSevereEnough(String severity) {
        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
    }
}
