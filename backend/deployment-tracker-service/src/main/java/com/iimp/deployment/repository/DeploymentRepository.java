package com.iimp.deployment.repository;

import com.iimp.incident.domain.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    Optional<Deployment> findByServiceNameAndVersion(String serviceName, String version);
}
