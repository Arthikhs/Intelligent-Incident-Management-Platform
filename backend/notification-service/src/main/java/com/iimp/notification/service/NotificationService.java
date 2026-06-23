package com.iimp.notification.service;

import com.iimp.notification.domain.Notification;
import com.iimp.notification.domain.Notification.NotificationStatus;
import com.iimp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final JavaMailSender mailSender;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendIncidentAlert(UUID incidentId, String severity, String title, String recipient) {
        String subject = "[IIMP ALERT] " + severity + " — " + title;
        String body = String.format("Incident detected.\nSeverity: %s\nTitle: %s\nIncident ID: %s", severity, title, incidentId);

        Notification notification = Notification.builder()
            .incidentId(incidentId)
            .channel("EMAIL")
            .recipient(recipient)
            .subject(subject)
            .body(body)
            .status(NotificationStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(recipient);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Notification sent to {} for incident {}", recipient, incidentId);
        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            log.error("Failed to send notification for incident {}: {}", incidentId, e.getMessage());
        }

        repository.save(notification);
    }

    public List<Notification> getByIncident(UUID incidentId) {
        return repository.findTop20ByIncidentIdOrderByCreatedAtDesc(incidentId);
    }
}
