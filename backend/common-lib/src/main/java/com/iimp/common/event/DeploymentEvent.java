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
public class DeploymentEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String serviceName;
    private String version;
    private String environment;
    private String status;         // IN_PROGRESS, SUCCESS, FAILED, ROLLED_BACK
    private String deployedBy;
    private String commitSha;
    private String branch;
    private String pipelineUrl;
    private Map<String, Object> configChanges;
    private Double riskScore;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startedAt = Instant.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;
}
