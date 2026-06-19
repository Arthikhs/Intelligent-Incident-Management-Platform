package com.iimp.health.repository;

import com.iimp.health.domain.ServiceHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceHealthRepository extends JpaRepository<ServiceHealth, UUID> {

    Optional<ServiceHealth> findTopByServiceNameOrderByEvaluatedAtDesc(String serviceName);

    @Query("""
        SELECT sh FROM ServiceHealth sh
        WHERE sh.evaluatedAt = (
            SELECT MAX(sh2.evaluatedAt) FROM ServiceHealth sh2
            WHERE sh2.serviceName = sh.serviceName
        )
        ORDER BY sh.healthScore ASC
        """)
    List<ServiceHealth> findLatestForAllServices();

    List<ServiceHealth> findByServiceNameOrderByEvaluatedAtDesc(String serviceName);
}
