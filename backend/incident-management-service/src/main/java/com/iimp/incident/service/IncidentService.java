package com.iimp.incident.service;

import com.iimp.common.dto.ApiResponse;
import com.iimp.common.event.IncidentEvent;
import com.iimp.common.exception.IIMPException;
import com.iimp.incident.domain.Incident;
import com.iimp.incident.domain.TimelineEvent;
import com.iimp.incident.dto.CreateIncidentRequest;
import com.iimp.incident.dto.DashboardSummary;
import com.iimp.incident.dto.IncidentResponse;
import com.iimp.incident.kafka.IncidentEventProducer;
import com.iimp.incident.mapper.IncidentMapper;
import com.iimp.incident.repository.AlertRepository;
import com.iimp.incident.repository.IncidentRepository;
import com.iimp.incident.repository.TimelineEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final TimelineEventRepository timelineRepository;
    private final IncidentMapper mapper;
    private final IncidentEventProducer eventProducer;
    private final MeterRegistry meterRegistry;

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request, UUID createdBy) {
        Incident incident = Incident.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .severity(request.getSeverity())
            .status(Incident.IncidentStatus.OPEN)
            .affectedServices(request.getAffectedServices())
            .tags(request.getTags() != null ? request.getTags() : List.of())
            .createdBy(createdBy)
            .build();

        incident = incidentRepository.save(incident);
        recordTimelineEvent(incident.getId(), "INCIDENT_STATUS", null,
            "Incident Created", "Incident opened with severity " + incident.getSeverity(), incident.getSeverity().name(), Instant.now());

        meterRegistry.counter("incidents.created", "severity", incident.getSeverity().name()).increment();

        IncidentEvent event = buildIncidentEvent(incident, "CREATED");
        eventProducer.publishIncidentEvent(event);

        log.info("Created incident: id={}, severity={}", incident.getId(), incident.getSeverity());
        return mapper.toResponse(incident);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(int page, int size) {
        return incidentRepository.findAll(PageRequest.of(page, size))
            .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getById(UUID id) {
        return incidentRepository.findById(id)
            .map(mapper::toResponse)
            .orElseThrow(() -> IIMPException.notFound("Incident", id.toString()));
    }

    @Transactional
    @CacheEvict(value = "incidents", key = "#id")
    public IncidentResponse acknowledge(UUID id, UUID userId) {
        Incident incident = findOrThrow(id);
        incident.acknowledge();
        incident = incidentRepository.save(incident);
        recordTimelineEvent(id, "INCIDENT_STATUS", null, "Incident Acknowledged",
            "Incident acknowledged by user " + userId, incident.getSeverity().name(), Instant.now());
        eventProducer.publishIncidentEvent(buildIncidentEvent(incident, "UPDATED"));
        return mapper.toResponse(incident);
    }

    @Transactional
    @CacheEvict(value = "incidents", key = "#id")
    public IncidentResponse resolve(UUID id, UUID userId) {
        Incident incident = findOrThrow(id);
        incident.resolve();
        incident = incidentRepository.save(incident);

        meterRegistry.counter("incidents.resolved", "severity", incident.getSeverity().name()).increment();
        if (incident.getMttrSeconds() != null) {
            meterRegistry.gauge("incidents.mttr.seconds", incident.getMttrSeconds());
        }

        recordTimelineEvent(id, "INCIDENT_STATUS", null, "Incident Resolved",
            "Incident resolved. MTTR=" + incident.getMttrSeconds() + "s", "INFO", Instant.now());
        eventProducer.publishIncidentEvent(buildIncidentEvent(incident, "RESOLVED"));
        return mapper.toResponse(incident);
    }

    @Transactional
    @CacheEvict(value = "incidents", key = "#id")
    public IncidentResponse close(UUID id) {
        Incident incident = findOrThrow(id);
        incident.close();
        incident = incidentRepository.save(incident);
        eventProducer.publishIncidentEvent(buildIncidentEvent(incident, "CLOSED"));
        return mapper.toResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<TimelineEvent> getTimeline(UUID incidentId) {
        findOrThrow(incidentId);
        return timelineRepository.findByIncidentIdOrderByOccurredAtAsc(incidentId);
    }

    @Transactional(readOnly = true)
    @Cacheable("dashboard-summary")
    public DashboardSummary getDashboardSummary() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Incident> activeIncidents = incidentRepository.findAllActive();
        Double avgMttr = incidentRepository.avgMttrSeconds(thirtyDaysAgo, Instant.now());

        Map<String, Long> byService = activeIncidents.stream()
            .flatMap(i -> i.getAffectedServices().stream())
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Long> byStatus = activeIncidents.stream()
            .collect(Collectors.groupingBy(i -> i.getStatus().name(), Collectors.counting()));

        Map<String, Long> bySeverity = activeIncidents.stream()
            .collect(Collectors.groupingBy(i -> i.getSeverity().name(), Collectors.counting()));

        return DashboardSummary.builder()
            .totalIncidents(incidentRepository.count())
            .activeIncidents(incidentRepository.countActiveIncidents())
            .criticalIncidents(bySeverity.getOrDefault("CRITICAL", 0L))
            .highIncidents(bySeverity.getOrDefault("HIGH", 0L))
            .avgMttrHours(avgMttr != null ? avgMttr / 3600.0 : 0.0)
            .incidentsByService(byService)
            .incidentsByStatus(byStatus)
            .incidentsBySeverity(bySeverity)
            .build();
    }

    private Incident findOrThrow(UUID id) {
        return incidentRepository.findById(id)
            .orElseThrow(() -> IIMPException.notFound("Incident", id.toString()));
    }

    private void recordTimelineEvent(UUID incidentId, String type, String service,
                                     String title, String description, String severity, Instant occurredAt) {
        TimelineEvent event = TimelineEvent.builder()
            .incidentId(incidentId)
            .eventType(type)
            .serviceName(service)
            .title(title)
            .description(description)
            .severity(severity)
            .occurredAt(occurredAt)
            .build();
        timelineRepository.save(event);
    }

    private IncidentEvent buildIncidentEvent(Incident incident, String eventType) {
        return IncidentEvent.builder()
            .incidentId(incident.getId().toString())
            .title(incident.getTitle())
            .severity(incident.getSeverity().name())
            .status(incident.getStatus().name())
            .affectedServices(incident.getAffectedServices())
            .eventType(eventType)
            .build();
    }
}
