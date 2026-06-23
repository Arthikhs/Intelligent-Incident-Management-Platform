package com.iimp.kb.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "knowledge_articles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KnowledgeArticle {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String category; // RUNBOOK, POSTMORTEM, RCA, ALERT

    @ElementCollection
    @CollectionTable(name = "kb_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    private List<String> tags;

    private String relatedService;
    private UUID relatedIncidentId;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;
}
