package com.iimp.postmortem.controller;

import com.iimp.postmortem.domain.Postmortem;
import com.iimp.postmortem.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/postmortems")
@RequiredArgsConstructor
public class PostmortemController {

    private final PostmortemService service;

    @GetMapping
    public ResponseEntity<List<Postmortem>> getPublished() {
        return ResponseEntity.ok(service.getPublished());
    }

    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<Postmortem> getByIncident(@PathVariable UUID incidentId) {
        return service.getByIncident(incidentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<Postmortem> generate(
            @RequestParam UUID incidentId,
            @RequestParam String title,
            @RequestParam(defaultValue = "") String rcaSummary,
            @RequestParam(defaultValue = "") String timeline,
            @RequestParam(defaultValue = "UNKNOWN") String severity) {
        return ResponseEntity.ok(service.generate(incidentId, title, rcaSummary, timeline, severity));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Postmortem> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(service.publish(id));
    }
}
