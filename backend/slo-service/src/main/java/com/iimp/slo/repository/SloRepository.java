package com.iimp.slo.repository;

import com.iimp.slo.domain.Slo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SloRepository extends JpaRepository<Slo, UUID> {
    Optional<Slo> findByServiceName(String serviceName);
    List<Slo> findByStatus(Slo.SloStatus status);
}
