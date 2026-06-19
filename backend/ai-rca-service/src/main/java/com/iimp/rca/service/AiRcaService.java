package com.iimp.rca.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iimp.common.exception.IIMPException;
import com.iimp.rca.domain.RcaReport;
import com.iimp.rca.dto.IncidentContext;
import com.iimp.rca.repository.RcaReportRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRcaService {

    private static final String RCA_PROMPT_TEMPLATE = """
        You are an expert Site Reliability Engineer (SRE) and Production Engineer performing a Root Cause Analysis.

        ## Incident Information
        - Incident ID: {incidentId}
        - Title: {title}
        - Severity: {severity}
        - Affected Services: {affectedServices}
        - Detected At: {detectedAt}

        ## Recent Error Logs (last 50 errors)
        {recentErrors}

        ## Recent Deployments (last 24 hours)
        {recentDeployments}

        ## Firing Alerts
        {firingAlerts}

        ## Error Traces
        {errorTraces}

        ## Service Health Metrics
        {serviceHealth}

        ## Your Task
        Analyze all the above signals and produce a structured Root Cause Analysis in the following JSON format ONLY. 
        Do NOT include any text outside the JSON block.

        {
          "rootCauses": [
            {
              "category": "DEPLOYMENT|CODE_BUG|INFRA|DEPENDENCY|CONFIG",
              "description": "Clear description of root cause",
              "evidence": "Specific log lines, metrics, or trace data that support this",
              "confidence": 0.0-1.0
            }
          ],
          "contributingFactors": ["factor1", "factor2"],
          "timelineSummary": "A concise narrative of how the incident unfolded",
          "impactSummary": "Description of business and technical impact",
          "recoveryActions": [
            {
              "priority": 1,
              "action": "Immediate action to take",
              "rationale": "Why this action is needed",
              "owner": "SRE|Developer|DevOps",
              "estimatedTime": "5 minutes"
            }
          ],
          "preventionRecs": [
            "Long-term recommendation to prevent recurrence"
          ],
          "confidenceScore": 0.0-1.0
        }
        """;

    private final ChatClient chatClient;
    private final RcaReportRepository rcaReportRepository;
    private final PromptBuilderService promptBuilderService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public RcaReport generateRca(IncidentContext context) {
        log.info("Starting AI RCA generation for incident: {}", context.getIncidentId());

        Timer.Sample sample = Timer.start(meterRegistry);
        long startMs = System.currentTimeMillis();

        RcaReport report = RcaReport.builder()
            .incidentId(context.getIncidentId())
            .status("IN_PROGRESS")
            .rootCauses(List.of())
            .contributingFactors(List.of())
            .recoveryActions(List.of())
            .preventionRecs(List.of())
            .build();
        report = rcaReportRepository.save(report);

        try {
            String promptContent = promptBuilderService.buildRcaPrompt(context, RCA_PROMPT_TEMPLATE);
            Prompt prompt = new Prompt(promptContent,
                OpenAiChatOptions.builder()
                    .withModel("gpt-4o")
                    .withTemperature(0.2f)
                    .withMaxTokens(4096)
                    .build()
            );

            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            String rawJson = chatResponse.getResult().getOutput().getContent();

            RcaParseResult parsed = parseRcaResponse(rawJson);
            Usage usage = chatResponse.getMetadata().getUsage();
            long generationMs = System.currentTimeMillis() - startMs;

            report.setRootCauses(parsed.rootCauses());
            report.setContributingFactors(parsed.contributingFactors());
            report.setTimelineSummary(parsed.timelineSummary());
            report.setImpactSummary(parsed.impactSummary());
            report.setRecoveryActions(parsed.recoveryActions());
            report.setPreventionRecs(parsed.preventionRecs());
            report.setConfidenceScore(BigDecimal.valueOf(parsed.confidenceScore()));
            report.setPromptTokens((int) usage.getPromptTokens());
            report.setCompletionTokens((int) usage.getGenerationTokens());
            report.setGenerationMs(generationMs);
            report.setStatus("COMPLETED");
            report.setGeneratedAt(Instant.now());

            report = rcaReportRepository.save(report);
            meterRegistry.counter("rca.generated", "status", "success").increment();
            sample.stop(meterRegistry.timer("rca.generation.duration"));

            log.info("RCA completed: incidentId={}, confidence={}, durationMs={}",
                context.getIncidentId(), parsed.confidenceScore(), generationMs);

            return report;

        } catch (Exception e) {
            log.error("RCA generation failed for incident: {}", context.getIncidentId(), e);
            report.setStatus("FAILED");
            report.setErrorMessage(e.getMessage());
            rcaReportRepository.save(report);
            meterRegistry.counter("rca.generated", "status", "failure").increment();
            throw IIMPException.internalError("RCA generation failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public RcaReport getByIncidentId(UUID incidentId) {
        return rcaReportRepository.findTopByIncidentIdOrderByGeneratedAtDesc(incidentId)
            .orElseThrow(() -> IIMPException.notFound("RCA Report", incidentId.toString()));
    }

    @Transactional(readOnly = true)
    public List<RcaReport> getAllByIncidentId(UUID incidentId) {
        return rcaReportRepository.findByIncidentIdOrderByGeneratedAtDesc(incidentId);
    }

    @SuppressWarnings("unchecked")
    private RcaParseResult parseRcaResponse(String rawJson) throws Exception {
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```"))     cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```"))       cleaned = cleaned.substring(0, cleaned.length() - 3);

        Map<String, Object> parsed = objectMapper.readValue(cleaned.trim(), Map.class);

        List<RcaReport.RootCause> rootCauses = objectMapper.convertValue(
            parsed.get("rootCauses"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, RcaReport.RootCause.class)
        );
        List<RcaReport.RecoveryAction> recoveryActions = objectMapper.convertValue(
            parsed.get("recoveryActions"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, RcaReport.RecoveryAction.class)
        );
        List<String> contributingFactors = (List<String>) parsed.getOrDefault("contributingFactors", List.of());
        List<String> preventionRecs      = (List<String>) parsed.getOrDefault("preventionRecs", List.of());
        String timelineSummary  = (String) parsed.getOrDefault("timelineSummary", "");
        String impactSummary    = (String) parsed.getOrDefault("impactSummary", "");
        double confidenceScore  = ((Number) parsed.getOrDefault("confidenceScore", 0.5)).doubleValue();

        return new RcaParseResult(rootCauses, contributingFactors, timelineSummary,
            impactSummary, recoveryActions, preventionRecs, confidenceScore);
    }

    private record RcaParseResult(
        List<RcaReport.RootCause> rootCauses,
        List<String> contributingFactors,
        String timelineSummary,
        String impactSummary,
        List<RcaReport.RecoveryAction> recoveryActions,
        List<String> preventionRecs,
        double confidenceScore
    ) {}
}
