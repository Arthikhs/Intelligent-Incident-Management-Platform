package com.iimp.slo.controller;

import com.iimp.slo.domain.Slo;
import com.iimp.slo.service.SloService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/slos")
@RequiredArgsConstructor
public class SloController {

    private final SloService service;

    @GetMapping
    public ResponseEntity<List<Slo>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<Slo> getByService(@PathVariable String serviceName) {
        return service.getByService(serviceName)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/breached")
    public ResponseEntity<List<Slo>> getBreached() {
        return ResponseEntity.ok(service.getBreached());
    }

    @PostMapping
    public ResponseEntity<Slo> create(
            @RequestParam String serviceName,
            @RequestParam String metricName,
            @RequestParam double targetPercent,
            @RequestParam(defaultValue = "30") int windowDays) {
        return ResponseEntity.ok(service.createOrUpdate(serviceName, metricName, targetPercent, windowDays));
    }

    @PostMapping("/{serviceName}/downtime")
    public ResponseEntity<Void> recordDowntime(
            @PathVariable String serviceName,
            @RequestParam double minutes) {
        service.recordDowntime(serviceName, minutes);
        return ResponseEntity.accepted().build();
    }
}
