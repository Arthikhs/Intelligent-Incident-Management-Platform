package com.iimp.kb.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_base")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KnowledgeEntry {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String category; // INCIDENT, RCA, RUNBOOK, POSTMORTEM

    private String tags;
    private String serviceName;
    private UUID incidentId;

    // Embedding vector stored as text (pgvector in production)
    @Column(columnDefinition = "TEXT")
    private String embeddingJson;

    private Double similarityScore;

    @Column(nullable = false)
    private Instant createdAt;
}
