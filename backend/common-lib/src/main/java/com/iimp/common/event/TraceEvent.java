package com.iimp.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private String status;         // OK, ERROR, TIMEOUT
    private Long durationMs;
    private String httpMethod;
    private String httpUrl;
    private Integer httpStatus;
    private String errorMessage;
    private Map<String, Object> attributes;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant spanStartedAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant spanEndedAt;
}
