package com.iimp.copilot.repository;

import com.iimp.copilot.domain.CopilotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CopilotMessageRepository extends JpaRepository<CopilotMessage, UUID> {
    List<CopilotMessage> findBySessionIdOrderByCreatedAt(String sessionId);
}
