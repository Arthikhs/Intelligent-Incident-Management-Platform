package com.iimp.logs.repository;

import com.iimp.logs.domain.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogRepository extends JpaRepository<Log, UUID> {

    List<Log> findByServiceNameAndLevelInAndLoggedAtAfterOrderByLoggedAtDesc(
        String serviceName, List<String> levels, Instant after
    );

    List<Log> findByIsAnomalyTrueAndLoggedAtAfterOrderByLoggedAtDesc(Instant after);

    long countByServiceNameAndLevelAndLoggedAtAfter(String serviceName, String level, Instant after);
}
