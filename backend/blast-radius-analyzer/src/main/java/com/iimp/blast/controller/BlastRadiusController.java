package com.iimp.blast.controller;

import com.iimp.blast.domain.BlastRadiusAnalysis;
import com.iimp.blast.service.BlastRadiusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blast-radius")
@RequiredArgsConstructor
public class BlastRadiusController {

    private final BlastRadiusService service;

    @PostMapping("/analyze")
    public ResponseEntity<BlastRadiusAnalysis> analyze(
            @RequestParam UUID incidentId,
            @RequestParam String originService) {
        return ResponseEntity.ok(service.analyze(incidentId, originService));
    }

    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<BlastRadiusAnalysis> getByIncident(@PathVariable UUID incidentId) {
        return service.getByIncidentId(incidentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
