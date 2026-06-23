package com.iimp.anomaly.controller;

import com.iimp.anomaly.domain.AnomalyEvent;
import com.iimp.anomaly.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyDetectionService service;

    @GetMapping("/{serviceName}")
    public ResponseEntity<List<AnomalyEvent>> getAnomalies(@PathVariable String serviceName) {
        return ResponseEntity.ok(service.getRecentAnomalies(serviceName));
    }

    @PostMapping("/detect/error-rate")
    public ResponseEntity<Void> detectErrorRate(
            @RequestParam String serviceName,
            @RequestParam double errorRate) {
        service.detectErrorRateAnomaly(serviceName, errorRate);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/detect/latency")
    public ResponseEntity<Void> detectLatency(
            @RequestParam String serviceName,
            @RequestParam double p99LatencyMs) {
        service.detectLatencyAnomaly(serviceName, p99LatencyMs);
        return ResponseEntity.accepted().build();
    }
}
