package com.iimp.postmortem.repository;

import com.iimp.postmortem.domain.Postmortem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostmortemRepository extends JpaRepository<Postmortem, UUID> {
    Optional<Postmortem> findByIncidentId(UUID incidentId);
    List<Postmortem> findByStatusOrderByCreatedAtDesc(Postmortem.PostmortemStatus status);
}
