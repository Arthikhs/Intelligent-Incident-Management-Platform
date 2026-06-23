package com.iimp.notification.controller;

import com.iimp.notification.domain.Notification;
import com.iimp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/incidents/{incidentId}")
    public ResponseEntity<List<Notification>> getByIncident(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(service.getByIncident(incidentId));
    }

    @PostMapping("/send")
    public ResponseEntity<Void> send(
            @RequestParam UUID incidentId,
            @RequestParam String severity,
            @RequestParam String title,
            @RequestParam String recipient) {
        service.sendIncidentAlert(incidentId, severity, title, recipient);
        return ResponseEntity.accepted().build();
    }
}
