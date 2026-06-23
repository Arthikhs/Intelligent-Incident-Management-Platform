package com.iimp.postmortem.service;

import com.iimp.postmortem.domain.Postmortem;
import com.iimp.postmortem.domain.Postmortem.PostmortemStatus;
import com.iimp.postmortem.repository.PostmortemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostmortemService {

    private final PostmortemRepository repository;
    private final ChatClient chatClient;

    @Transactional
    public Postmortem generate(UUID incidentId, String title, String rcaSummary,
                                String timelineJson, String severity) {

        Optional<Postmortem> existing = repository.findByIncidentId(incidentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String prompt = String.format("""
            You are an SRE writing a blameless postmortem. Given the following incident details, produce a structured postmortem.

            Incident ID: %s
            Title: %s
            Severity: %s
            RCA Summary: %s
            Timeline: %s

            Respond in this exact format:
            SUMMARY: <2-3 sentence summary>
            ROOT_CAUSE: <root cause>
            CONTRIBUTING_FACTORS: <bullet list>
            ACTION_ITEMS: <numbered list of follow-up tasks>
            LESSONS_LEARNED: <key takeaways>
            """,
            incidentId, title, severity, rcaSummary, timelineJson);

        String response = chatClient.prompt().user(prompt).call().content();

        Postmortem postmortem = Postmortem.builder()
            .incidentId(incidentId)
            .title("Postmortem: " + title)
            .summary(extract(response, "SUMMARY"))
            .timeline(timelineJson)
            .rootCause(extract(response, "ROOT_CAUSE"))
            .contributingFactors(extract(response, "CONTRIBUTING_FACTORS"))
            .actionItems(extract(response, "ACTION_ITEMS"))
            .lessonsLearned(extract(response, "LESSONS_LEARNED"))
            .status(PostmortemStatus.DRAFT)
            .generatedBy("AI")
            .aiConfidenceScore(0.85)
            .createdAt(Instant.now())
            .build();

        log.info("Generated postmortem for incident {}", incidentId);
        return repository.save(postmortem);
    }

    @Transactional
    public Postmortem publish(UUID id) {
        Postmortem p = repository.findById(id).orElseThrow();
        p.setStatus(PostmortemStatus.PUBLISHED);
        p.setPublishedAt(Instant.now());
        return repository.save(p);
    }

    public Optional<Postmortem> getByIncident(UUID incidentId) {
        return repository.findByIncidentId(incidentId);
    }

    public List<Postmortem> getPublished() {
        return repository.findByStatusOrderByCreatedAtDesc(PostmortemStatus.PUBLISHED);
    }

    private String extract(String text, String field) {
        String marker = field + ":";
        StringBuilder result = new StringBuilder();
        boolean capturing = false;
        for (String line : text.split("\n")) {
            if (line.startsWith(marker)) {
                result.append(line.substring(marker.length()).trim());
                capturing = true;
            } else if (capturing) {
                if (line.startsWith("SUMMARY:") || line.startsWith("ROOT_CAUSE:") ||
                    line.startsWith("CONTRIBUTING_FACTORS:") || line.startsWith("ACTION_ITEMS:") ||
                    line.startsWith("LESSONS_LEARNED:")) break;
                result.append("\n").append(line);
            }
        }
        return result.toString().trim();
    }
}
