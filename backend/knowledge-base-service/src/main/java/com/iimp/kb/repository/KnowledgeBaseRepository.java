package com.iimp.kb.repository;

import com.iimp.kb.domain.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeEntry, UUID> {
    List<KnowledgeEntry> findByCategoryOrderByCreatedAtDesc(String category);
}
