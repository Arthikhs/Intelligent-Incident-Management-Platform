package com.iimp.notification.repository;

import com.iimp.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop20ByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
