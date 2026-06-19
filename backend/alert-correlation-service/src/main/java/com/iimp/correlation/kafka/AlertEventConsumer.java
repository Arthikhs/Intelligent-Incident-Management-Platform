package com.iimp.correlation.kafka;

import com.iimp.common.event.AlertEvent;
import com.iimp.correlation.service.AlertCorrelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final AlertCorrelationService correlationService;

    @KafkaListener(topics = "alerts-topic", groupId = "alert-correlation-service",
                   concurrency = "3", containerFactory = "kafkaListenerContainerFactory")
    public void onAlert(AlertEvent event) {
        log.debug("Processing alert: service={}, alert={}", event.getServiceName(), event.getAlertName());
        correlationService.processAlert(event);
    }
}
