package com.iimp.copilot.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "copilot_messages")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CopilotMessage {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String role; // USER, ASSISTANT

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private UUID incidentId;

    @Column(nullable = false)
    private Instant createdAt;
}
