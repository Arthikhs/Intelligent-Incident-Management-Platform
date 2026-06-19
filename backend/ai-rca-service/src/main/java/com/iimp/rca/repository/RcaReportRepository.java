package com.iimp.rca.repository;

import com.iimp.rca.domain.RcaReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RcaReportRepository extends JpaRepository<RcaReport, UUID> {

    Optional<RcaReport> findTopByIncidentIdOrderByGeneratedAtDesc(UUID incidentId);

    List<RcaReport> findByIncidentIdOrderByGeneratedAtDesc(UUID incidentId);
}
