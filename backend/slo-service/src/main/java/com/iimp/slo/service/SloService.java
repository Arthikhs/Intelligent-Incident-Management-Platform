package com.iimp.slo.service;

import com.iimp.slo.domain.Slo;
import com.iimp.slo.domain.Slo.SloStatus;
import com.iimp.slo.repository.SloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SloService {

    private final SloRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Slo createOrUpdate(String serviceName, String metricName, double targetPercent, int windowDays) {
        double totalMinutes = windowDays * 24 * 60.0;
        double errorBudget = totalMinutes * (1 - targetPercent / 100.0);

        Slo slo = repository.findByServiceName(serviceName).orElse(
            Slo.builder()
                .serviceName(serviceName)
                .metricName(metricName)
                .targetPercent(targetPercent)
                .windowDays(String.valueOf(windowDays))
                .errorBudgetMinutes(errorBudget)
                .errorBudgetRemainingMinutes(errorBudget)
                .currentCompliancePercent(100.0)
                .status(SloStatus.OK)
                .updatedAt(Instant.now())
                .build()
        );

        slo.setTargetPercent(targetPercent);
        slo.setErrorBudgetMinutes(errorBudget);
        slo.setUpdatedAt(Instant.now());
        return repository.save(slo);
    }

    @Transactional
    public void recordDowntime(String serviceName, double downtimeMinutes) {
        repository.findByServiceName(serviceName).ifPresent(slo -> {
            double remaining = Math.max(0, slo.getErrorBudgetRemainingMinutes() - downtimeMinutes);
            double totalMinutes = Integer.parseInt(slo.getWindowDays()) * 24 * 60.0;
            double consumed = slo.getErrorBudgetMinutes() - remaining;
            double compliance = 100.0 - (consumed / totalMinutes * 100.0);

            slo.setErrorBudgetRemainingMinutes(remaining);
            slo.setCurrentCompliancePercent(Math.max(0, compliance));
            slo.setStatus(remaining == 0 ? SloStatus.BREACHED
                        : remaining < slo.getErrorBudgetMinutes() * 0.1 ? SloStatus.WARNING
                        : SloStatus.OK);
            slo.setUpdatedAt(Instant.now());
            repository.save(slo);

            if (slo.getStatus() != SloStatus.OK) {
                kafkaTemplate.send("slo-breach-topic", serviceName, Map.of(
                    "serviceName", serviceName,
                    "status", slo.getStatus().name(),
                    "remainingBudgetMinutes", remaining,
                    "compliancePercent", slo.getCurrentCompliancePercent()
                ));
                log.warn("SLO {} for service {}: budget remaining={} min", slo.getStatus(), serviceName, String.format("%.2f", remaining));
            }
        });
    }

    public Optional<Slo> getByService(String serviceName) {
        return repository.findByServiceName(serviceName);
    }

    public List<Slo> getAll() {
        return repository.findAll();
    }

    public List<Slo> getBreached() {
        return repository.findByStatus(SloStatus.BREACHED);
    }
}
