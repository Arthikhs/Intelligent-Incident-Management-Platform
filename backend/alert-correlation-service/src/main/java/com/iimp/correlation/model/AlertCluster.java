package com.iimp.correlation.model;

import com.iimp.common.event.AlertEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AlertCluster {
    private String clusterId;
    private String serviceName;
    private String severity;
    private List<AlertEvent> alerts;
    private Instant windowStart;
    @Builder.Default
    private boolean incidentCreated = false;
}
