package com.iimp.inventory;

import com.iimp.common.event.DeploymentEvent;
import com.iimp.common.event.LogEvent;
import com.iimp.common.event.TraceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@EnableScheduling
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}

@RestController
class InventoryController {
    @GetMapping("/api/v1/inventory/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "inventory-service");
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class InventorySignalEmitter {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    private final AtomicInteger deployVersion = new AtomicInteger(1);

    private static final List<String> OPERATIONS = List.of(
        "GET /api/v1/inventory/{sku}", "PUT /api/v1/inventory/{sku}/stock", "POST /api/v1/inventory/reserve"
    );

    @Scheduled(fixedDelay = 6000)
    public void emitTraces() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String spanId  = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long   latency = 20 + random.nextInt(200);
        boolean isError = random.nextDouble() < 0.03; // 3% error rate

        TraceEvent trace = TraceEvent.builder()
            .traceId(traceId)
            .spanId(spanId)
            .serviceName("inventory-service")
            .operationName(OPERATIONS.get(random.nextInt(OPERATIONS.size())))
            .status(isError ? "ERROR" : "OK")
            .durationMs(latency)
            .httpMethod("GET")
            .httpStatus(isError ? 500 : 200)
            .spanStartedAt(Instant.now().minusMillis(latency))
            .spanEndedAt(Instant.now())
            .build();

        kafkaTemplate.send("traces-topic", traceId, trace);

        if (isError) {
            LogEvent logEvent = LogEvent.builder()
                .serviceName("inventory-service")
                .level("ERROR")
                .message("Stock lookup failed: Database connection timeout")
                .exceptionClass("com.zaxxer.hikari.pool.HikariPool$PoolInitializationException")
                .traceId(traceId)
                .loggedAt(Instant.now())
                .build();
            kafkaTemplate.send("logs-topic", traceId, logEvent);
        }
    }

    // Emit a deployment event every 5 minutes to simulate rolling deploys
    @Scheduled(fixedDelay = 300000)
    public void emitDeploymentEvent() {
        int version = deployVersion.incrementAndGet();
        String status = random.nextDouble() < 0.8 ? "SUCCESS" : "FAILED";

        DeploymentEvent event = DeploymentEvent.builder()
            .serviceName("inventory-service")
            .version("1." + version + ".0")
            .environment("production")
            .status(status)
            .deployedBy("ci-pipeline")
            .commitSha(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
            .branch("main")
            .startedAt(Instant.now().minusSeconds(120))
            .completedAt(Instant.now())
            .build();

        kafkaTemplate.send("deployment-topic", event.getServiceName(), event);
        log.info("Emitted deployment event: service=inventory-service, version={}, status={}", version, status);
    }
}
