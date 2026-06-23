package com.iimp.copilot.controller;

import com.iimp.copilot.domain.CopilotMessage;
import com.iimp.copilot.service.CopilotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService service;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) UUID incidentId) {
        String response = service.chat(sessionId, message, incidentId);
        return ResponseEntity.ok(Map.of("response", response, "sessionId", sessionId));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<CopilotMessage>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(service.getHistory(sessionId));
    }
}
