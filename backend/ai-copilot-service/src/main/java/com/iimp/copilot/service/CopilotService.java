package com.iimp.copilot.service;

import com.iimp.copilot.domain.CopilotMessage;
import com.iimp.copilot.repository.CopilotMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotService {

    private final CopilotMessageRepository repository;
    private final ChatClient chatClient;

    @Transactional
    public String chat(String sessionId, String userMessage, UUID incidentId) {
        List<Message> history = repository.findTop20BySessionIdOrderByCreatedAtAsc(sessionId).stream()
            .map(m -> (Message) ("USER".equals(m.getRole())
                ? new UserMessage(m.getContent())
                : new AssistantMessage(m.getContent())))
            .toList();

        // Save user message
        repository.save(CopilotMessage.builder()
            .sessionId(sessionId)
            .role("USER")
            .content(userMessage)
            .incidentId(incidentId)
            .createdAt(Instant.now())
            .build());

        // Call AI with history context
        String response = chatClient.prompt()
            .messages(history)
            .user(userMessage)
            .call()
            .content();

        // Save assistant response
        repository.save(CopilotMessage.builder()
            .sessionId(sessionId)
            .role("ASSISTANT")
            .content(response)
            .incidentId(incidentId)
            .createdAt(Instant.now())
            .build());

        log.debug("Copilot session={} responded", sessionId);
        return response;
    }

    public List<CopilotMessage> getHistory(String sessionId) {
        return repository.findBySessionIdOrderByCreatedAt(sessionId);
    }
}
