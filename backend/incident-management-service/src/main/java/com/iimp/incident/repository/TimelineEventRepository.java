package com.iimp.incident.repository;

import com.iimp.incident.domain.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    List<TimelineEvent> findByIncidentIdOrderByOccurredAtAsc(UUID incidentId);

    List<TimelineEvent> findByIncidentIdAndEventTypeOrderByOccurredAtAsc(UUID incidentId, String eventType);
}
