package com.iimp.kb.service;

import com.iimp.kb.domain.KnowledgeArticle;
import com.iimp.kb.repository.KnowledgeArticleRepository;
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
public class KnowledgeBaseService {

    private final KnowledgeArticleRepository repository;
    private final ChatClient chatClient;

    @Transactional
    public KnowledgeArticle create(KnowledgeArticle article) {
        article.setCreatedAt(Instant.now());
        return repository.save(article);
    }

    public List<KnowledgeArticle> search(String query) {
        return repository.search(query);
    }

    public List<KnowledgeArticle> getByService(String serviceName) {
        return repository.findByRelatedService(serviceName);
    }

    public List<KnowledgeArticle> getByCategory(String category) {
        return repository.findByCategory(category);
    }

    public Optional<KnowledgeArticle> getById(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public KnowledgeArticle generateRunbook(String serviceName, String problemDescription) {
        String prompt = String.format("""
            Generate a concise SRE runbook for the following problem.
            Service: %s
            Problem: %s

            Format:
            ## Overview
            ## Detection
            ## Investigation Steps
            ## Resolution Steps
            ## Prevention
            """, serviceName, problemDescription);

        String content = chatClient.prompt().user(prompt).call().content();

        KnowledgeArticle article = KnowledgeArticle.builder()
            .title("Runbook: " + problemDescription)
            .content(content)
            .category("RUNBOOK")
            .relatedService(serviceName)
            .tags(List.of(serviceName, "runbook", "ai-generated"))
            .createdBy("AI")
            .createdAt(Instant.now())
            .build();

        log.info("Generated runbook for service={} problem={}", serviceName, problemDescription);
        return repository.save(article);
    }
}
