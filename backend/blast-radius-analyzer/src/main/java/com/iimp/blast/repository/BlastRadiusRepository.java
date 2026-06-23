package com.iimp.blast.repository;

import com.iimp.blast.domain.BlastRadiusAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BlastRadiusRepository extends JpaRepository<BlastRadiusAnalysis, UUID> {
    Optional<BlastRadiusAnalysis> findTopByIncidentIdOrderByAnalyzedAtDesc(UUID incidentId);
}
