package com.iimp.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String serviceName;
    private String level;          // TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    private String message;
    private String loggerName;
    private String threadName;
    private String traceId;
    private String spanId;
    private String exceptionClass;
    private String stackTrace;
    private Object fields;         // arbitrary key-value pairs

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant loggedAt = Instant.now();
}
