package com.iimp.logs.kafka;

import com.iimp.common.event.LogEvent;
import com.iimp.logs.service.LogAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventConsumer {

    private final LogAnalyzerService logAnalyzerService;

    @KafkaListener(topics = "logs-topic", groupId = "log-analyzer-service",
                   concurrency = "3", containerFactory = "kafkaListenerContainerFactory")
    public void onLogEvent(LogEvent event) {
        log.debug("Processing log: service={}, level={}", event.getServiceName(), event.getLevel());
        logAnalyzerService.processLog(event);
    }
}
