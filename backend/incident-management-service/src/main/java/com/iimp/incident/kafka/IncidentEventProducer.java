package com.iimp.incident.kafka;

import com.iimp.common.event.IncidentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventProducer {

    private static final String INCIDENTS_TOPIC = "incidents-topic";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishIncidentEvent(IncidentEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(INCIDENTS_TOPIC, event.getIncidentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish incident event: incidentId={}, error={}",
                    event.getIncidentId(), ex.getMessage());
            } else {
                log.debug("Published incident event: incidentId={}, partition={}, offset={}",
                    event.getIncidentId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
