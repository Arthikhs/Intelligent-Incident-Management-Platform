package com.iimp.incident.repository;

import com.iimp.incident.domain.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByIncidentId(UUID incidentId);

    Page<Alert> findByServiceNameOrderByFiredAtDesc(String serviceName, Pageable pageable);

    List<Alert> findByFingerprintAndStatus(String fingerprint, String status);

    List<Alert> findByStatusOrderByFiredAtDesc(String status);

    long countByIncidentId(UUID incidentId);
}
