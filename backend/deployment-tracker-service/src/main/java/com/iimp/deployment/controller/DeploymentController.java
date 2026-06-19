package com.iimp.deployment.controller;

import com.iimp.incident.domain.Deployment;
import com.iimp.deployment.repository.DeploymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
@Tag(name = "Deployments", description = "Deployment Tracking API")
public class DeploymentController {

    private final DeploymentRepository deploymentRepository;

    @GetMapping
    @Operation(summary = "List all deployments")
    public List<Deployment> listDeployments(
        @RequestParam(defaultValue = "20") int limit
    ) {
        return deploymentRepository.findAll()
            .stream()
            .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
            .limit(limit)
            .toList();
    }
}
