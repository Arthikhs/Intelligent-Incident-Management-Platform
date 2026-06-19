package com.iimp.incident.kafka;

import com.iimp.common.event.AlertEvent;
import com.iimp.incident.domain.Alert;
import com.iimp.incident.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final AlertRepository alertRepository;

    @KafkaListener(topics = "alerts-topic", groupId = "incident-management-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onAlertEvent(AlertEvent event) {
        log.info("Received alert: service={}, alert={}, severity={}",
            event.getServiceName(), event.getAlertName(), event.getSeverity());

        Alert alert = Alert.builder()
            .serviceName(event.getServiceName())
            .alertName(event.getAlertName())
            .severity(event.getSeverity())
            .status(event.getStatus())
            .source(event.getSource())
            .message(event.getMessage())
            .labels(event.getLabels())
            .annotations(event.getAnnotations())
            .fingerprint(event.getFingerprint())
            .firedAt(event.getFiredAt())
            .build();

        alertRepository.save(alert);
        log.debug("Persisted alert: fingerprint={}", event.getFingerprint());
    }
}
