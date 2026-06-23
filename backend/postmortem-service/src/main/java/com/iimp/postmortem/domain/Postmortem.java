package com.iimp.postmortem.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "postmortems")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Postmortem {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID incidentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String contributingFactors;

    @Column(columnDefinition = "TEXT")
    private String actionItems;

    @Column(columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostmortemStatus status;

    private String generatedBy; // AI or MANUAL
    private Double aiConfidenceScore;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    public enum PostmortemStatus { DRAFT, REVIEW, PUBLISHED }
}
