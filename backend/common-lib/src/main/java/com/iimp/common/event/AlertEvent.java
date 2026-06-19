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
public class AlertEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String serviceName;
    private String alertName;
    private String severity;       // CRITICAL, HIGH, MEDIUM, LOW, INFO
    private String status;         // FIRING, RESOLVED
    private String source;
    private String message;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private String fingerprint;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant firedAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant resolvedAt;
}
