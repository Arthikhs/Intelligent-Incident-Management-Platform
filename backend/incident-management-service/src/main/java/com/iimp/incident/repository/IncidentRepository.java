package com.iimp.incident.repository;

import com.iimp.incident.domain.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findByStatusOrderByDetectedAtDesc(Incident.IncidentStatus status, Pageable pageable);

    Page<Incident> findBySeverityOrderByDetectedAtDesc(Incident.Severity severity, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE :service = ANY(i.affectedServices) ORDER BY i.detectedAt DESC")
    List<Incident> findByAffectedService(@Param("service") String service);

    @Query("SELECT i FROM Incident i WHERE i.detectedAt BETWEEN :from AND :to ORDER BY i.detectedAt DESC")
    Page<Incident> findByDateRange(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status NOT IN ('RESOLVED','CLOSED')")
    long countActiveIncidents();

    @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('RESOLVED','CLOSED') ORDER BY i.severity, i.detectedAt DESC")
    List<Incident> findAllActive();

    @Query("SELECT AVG(i.mttrSeconds) FROM Incident i WHERE i.mttrSeconds IS NOT NULL AND i.resolvedAt BETWEEN :from AND :to")
    Double avgMttrSeconds(@Param("from") Instant from, @Param("to") Instant to);
}
