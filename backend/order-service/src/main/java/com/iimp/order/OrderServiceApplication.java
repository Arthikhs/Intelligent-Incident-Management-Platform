package com.iimp.order;

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

@SpringBootApplication
@EnableScheduling
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

@RestController
class OrderController {
    @GetMapping("/api/v1/orders/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "order-service");
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class OrderSignalEmitter {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    private static final List<String> OPERATIONS = List.of(
        "POST /api/v1/orders", "GET /api/v1/orders/{id}", "PUT /api/v1/orders/{id}/status"
    );

    @Scheduled(fixedDelay = 5000)
    public void emitTraces() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String spanId  = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long   latency = 50 + random.nextInt(450);
        boolean isError = random.nextDouble() < 0.05; // 5% error rate

        TraceEvent trace = TraceEvent.builder()
            .traceId(traceId)
            .spanId(spanId)
            .serviceName("order-service")
            .operationName(OPERATIONS.get(random.nextInt(OPERATIONS.size())))
            .status(isError ? "ERROR" : "OK")
            .durationMs(latency)
            .httpMethod("POST")
            .httpStatus(isError ? 500 : 200)
            .spanStartedAt(Instant.now().minusMillis(latency))
            .spanEndedAt(Instant.now())
            .build();

        kafkaTemplate.send("traces-topic", traceId, trace);

        if (isError) {
            LogEvent logEvent = LogEvent.builder()
                .serviceName("order-service")
                .level("ERROR")
                .message("Order processing failed: Internal server error during order creation")
                .traceId(traceId)
                .spanId(spanId)
                .loggedAt(Instant.now())
                .build();
            kafkaTemplate.send("logs-topic", traceId, logEvent);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void emitInfoLog() {
        LogEvent logEvent = LogEvent.builder()
            .serviceName("order-service")
            .level("INFO")
            .message("Order service heartbeat — processed " + (100 + random.nextInt(200)) + " orders in last 30s")
            .loggedAt(Instant.now())
            .build();
        kafkaTemplate.send("logs-topic", UUID.randomUUID().toString(), logEvent);
    }
}
