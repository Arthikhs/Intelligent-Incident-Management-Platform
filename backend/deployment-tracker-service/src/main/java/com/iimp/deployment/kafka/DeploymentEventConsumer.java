package com.iimp.deployment.kafka;

import com.iimp.common.event.DeploymentEvent;
import com.iimp.deployment.service.DeploymentTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentEventConsumer {

    private final DeploymentTrackerService deploymentTrackerService;

    @KafkaListener(topics = "deployment-topic", groupId = "deployment-tracker-service",
                   concurrency = "2", containerFactory = "kafkaListenerContainerFactory")
    public void onDeploymentEvent(DeploymentEvent event) {
        log.debug("Processing deployment: service={}, version={}, status={}",
            event.getServiceName(), event.getVersion(), event.getStatus());
        deploymentTrackerService.processDeploymentEvent(event);
    }
}
