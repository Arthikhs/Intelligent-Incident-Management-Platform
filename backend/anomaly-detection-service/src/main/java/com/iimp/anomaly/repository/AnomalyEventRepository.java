package com.iimp.anomaly.repository;

import com.iimp.anomaly.domain.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, UUID> {
    List<AnomalyEvent> findTop20ByServiceNameOrderByDetectedAtDesc(String serviceName);
}
