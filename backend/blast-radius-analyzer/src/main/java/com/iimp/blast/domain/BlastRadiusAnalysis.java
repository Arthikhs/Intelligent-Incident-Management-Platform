package com.iimp.blast.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blast_radius_analyses")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BlastRadiusAnalysis {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String originService;

    @ElementCollection
    @CollectionTable(name = "blast_impacted_services", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "service_name")
    private List<String> impactedServices;

    @ElementCollection
    @CollectionTable(name = "blast_impacted_apis", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "api_path")
    private List<String> impactedApis;

    private Integer estimatedAffectedUsers;
    private String businessImpact;
    private String impactLevel; // CRITICAL, HIGH, MEDIUM, LOW
    private Double propagationScore;

    @Column(nullable = false)
    private Instant analyzedAt;
}
