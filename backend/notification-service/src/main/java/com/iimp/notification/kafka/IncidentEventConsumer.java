package com.iimp.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.iimp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventConsumer {

    private final NotificationService notificationService;

    @Value("${notification.default-recipient:ops-team@iimp.io}")
    private String defaultRecipient;

    @KafkaListener(topics = "incidents-topic", groupId = "notification-group")
    public void onIncident(JsonNode event) {
        try {
            String type = event.path("eventType").asText("");
            if (!"INCIDENT_CREATED".equals(type)) return;

            UUID incidentId = UUID.fromString(event.path("incidentId").asText());
            String severity = event.path("severity").asText("UNKNOWN");
            String title = event.path("title").asText("Incident");

            notificationService.sendIncidentAlert(incidentId, severity, title, defaultRecipient);
        } catch (Exception e) {
            log.error("Failed to process incident event for notification", e);
        }
    }
}
