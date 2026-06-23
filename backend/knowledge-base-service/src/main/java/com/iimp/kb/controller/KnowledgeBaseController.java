package com.iimp.kb.controller;

import com.iimp.kb.domain.KnowledgeArticle;
import com.iimp.kb.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;

    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeArticle>> search(@RequestParam String q) {
        return ResponseEntity.ok(service.search(q));
    }

    @GetMapping("/services/{serviceName}")
    public ResponseEntity<List<KnowledgeArticle>> getByService(@PathVariable String serviceName) {
        return ResponseEntity.ok(service.getByService(serviceName));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<KnowledgeArticle>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(service.getByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeArticle> getById(@PathVariable UUID id) {
        return service.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<KnowledgeArticle> create(@RequestBody KnowledgeArticle article) {
        return ResponseEntity.ok(service.create(article));
    }

    @PostMapping("/runbook/generate")
    public ResponseEntity<KnowledgeArticle> generateRunbook(
            @RequestParam String serviceName,
            @RequestParam String problemDescription) {
        return ResponseEntity.ok(service.generateRunbook(serviceName, problemDescription));
    }
}
