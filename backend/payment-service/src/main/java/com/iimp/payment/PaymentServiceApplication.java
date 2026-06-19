package com.iimp.payment;

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
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

@RestController
class PaymentController {
    @GetMapping("/api/v1/payments/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "payment-service");
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class PaymentSignalEmitter {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    private static final List<String> OPERATIONS = List.of(
        "POST /api/v1/payments/charge", "POST /api/v1/payments/refund", "GET /api/v1/payments/{id}"
    );

    private static final List<String> EXCEPTIONS = List.of(
        "com.iimp.payment.PaymentGatewayException",
        "java.net.SocketTimeoutException",
        "com.iimp.payment.InsufficientFundsException"
    );

    @Scheduled(fixedDelay = 4000)
    public void emitTraces() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String spanId  = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long   latency = 80 + random.nextInt(800);
        boolean isError   = random.nextDouble() < 0.08; // 8% error rate
        boolean isTimeout = !isError && random.nextDouble() < 0.03;

        TraceEvent trace = TraceEvent.builder()
            .traceId(traceId)
            .spanId(spanId)
            .serviceName("payment-service")
            .operationName(OPERATIONS.get(random.nextInt(OPERATIONS.size())))
            .status(isError ? "ERROR" : isTimeout ? "TIMEOUT" : "OK")
            .durationMs(isTimeout ? 30000L : latency)
            .httpMethod("POST")
            .httpStatus(isError ? 500 : isTimeout ? 504 : 200)
            .errorMessage(isError ? "Payment gateway unavailable" : null)
            .spanStartedAt(Instant.now().minusMillis(latency))
            .spanEndedAt(Instant.now())
            .build();

        kafkaTemplate.send("traces-topic", traceId, trace);

        if (isError) {
            String exClass = EXCEPTIONS.get(random.nextInt(EXCEPTIONS.size()));
            LogEvent logEvent = LogEvent.builder()
                .serviceName("payment-service")
                .level("ERROR")
                .message("Payment processing failed: " + exClass)
                .exceptionClass(exClass)
                .traceId(traceId)
                .spanId(spanId)
                .loggedAt(Instant.now())
                .build();
            kafkaTemplate.send("logs-topic", traceId, logEvent);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void emitInfoLog() {
        LogEvent logEvent = LogEvent.builder()
            .serviceName("payment-service")
            .level("INFO")
            .message("Payment service heartbeat — processed " + (50 + random.nextInt(100)) + " transactions in last 60s")
            .loggedAt(Instant.now())
            .build();
        kafkaTemplate.send("logs-topic", UUID.randomUUID().toString(), logEvent);
    }
}
