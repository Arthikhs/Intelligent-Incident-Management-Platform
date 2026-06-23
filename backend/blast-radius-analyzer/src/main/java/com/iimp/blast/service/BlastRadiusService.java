package com.iimp.blast.service;

import com.iimp.blast.domain.BlastRadiusAnalysis;
import com.iimp.blast.repository.BlastRadiusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlastRadiusService {

    private final BlastRadiusRepository repository;

    // Service dependency graph — in production, derived from traces/topology
    private static final Map<String, List<String>> DEPENDENCY_GRAPH = Map.of(
        "api-gateway",       List.of("order-service", "payment-service", "inventory-service", "user-service"),
        "order-service",     List.of("payment-service", "inventory-service"),
        "payment-service",   List.of("inventory-service"),
        "inventory-service", List.of()
    );

    @Transactional
    public BlastRadiusAnalysis analyze(UUID incidentId, String originService) {
        log.info("Analyzing blast radius for incident={} origin={}", incidentId, originService);

        List<String> impacted = new ArrayList<>();
        collectImpacted(originService, impacted, new HashSet<>());

        double propagationScore = impacted.size() / (double) Math.max(DEPENDENCY_GRAPH.size(), 1);
        String impactLevel = propagationScore >= 0.75 ? "CRITICAL"
                           : propagationScore >= 0.5  ? "HIGH"
                           : propagationScore >= 0.25 ? "MEDIUM" : "LOW";

        BlastRadiusAnalysis analysis = BlastRadiusAnalysis.builder()
            .incidentId(incidentId)
            .originService(originService)
            .impactedServices(impacted)
            .impactedApis(deriveImpactedApis(impacted))
            .estimatedAffectedUsers(estimateUsers(impactLevel))
            .businessImpact(buildBusinessImpact(originService, impacted))
            .impactLevel(impactLevel)
            .propagationScore(propagationScore)
            .analyzedAt(Instant.now())
            .build();

        return repository.save(analysis);
    }

    @Transactional(readOnly = true)
    public Optional<BlastRadiusAnalysis> getByIncidentId(UUID incidentId) {
        return repository.findTopByIncidentIdOrderByAnalyzedAtDesc(incidentId);
    }

    private void collectImpacted(String service, List<String> result, Set<String> visited) {
        if (visited.contains(service)) return;
        visited.add(service);
        DEPENDENCY_GRAPH.getOrDefault(service, List.of()).forEach(dep -> {
            result.add(dep);
            collectImpacted(dep, result, visited);
        });
    }

    private List<String> deriveImpactedApis(List<String> services) {
        Map<String, List<String>> apiMap = Map.of(
            "order-service",   List.of("POST /api/v1/orders", "GET /api/v1/orders"),
            "payment-service", List.of("POST /api/v1/payments"),
            "inventory-service", List.of("GET /api/v1/inventory")
        );
        return services.stream()
            .flatMap(s -> apiMap.getOrDefault(s, List.of()).stream())
            .distinct().toList();
    }

    private int estimateUsers(String impactLevel) {
        return switch (impactLevel) {
            case "CRITICAL" -> 10000;
            case "HIGH"     -> 5000;
            case "MEDIUM"   -> 1000;
            default         -> 100;
        };
    }

    private String buildBusinessImpact(String origin, List<String> impacted) {
        return String.format("Service '%s' failure propagates to %d downstream services: %s",
            origin, impacted.size(), String.join(", ", impacted));
    }
}
