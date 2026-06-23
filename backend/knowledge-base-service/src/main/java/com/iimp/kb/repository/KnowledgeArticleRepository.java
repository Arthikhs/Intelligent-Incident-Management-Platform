package com.iimp.kb.repository;

import com.iimp.kb.domain.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {

    List<KnowledgeArticle> findByRelatedService(String serviceName);

    @Query("SELECT a FROM KnowledgeArticle a WHERE " +
           "LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<KnowledgeArticle> search(@Param("q") String query);

    List<KnowledgeArticle> findByCategory(String category);
}
