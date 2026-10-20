package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.ai.AiAdvisoryClient;
import com.adtech.iaterap.backend.dto.*;
import com.adtech.iaterap.backend.entity.*;
import com.adtech.iaterap.backend.repository.AiSuggestionQueueRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/** Persists advisory output and requires an explicit, attributable engineer decision. */
@Service
public class AiSuggestionQueueService {
    private final AiAdvisoryClient client;
    private final AiSuggestionQueueRepository repository;
    private final AuditService auditService;
    public AiSuggestionQueueService(AiAdvisoryClient client, AiSuggestionQueueRepository repository, AuditService auditService) { this.client = client; this.repository = repository; this.auditService = auditService; }
    @Transactional
    public AiSuggestionQueueResponse enqueue(AiSuggestionRequest request, String engineer) {
        AiSuggestionResponse result = client.suggest(request);
        AiSuggestionQueueItem item = new AiSuggestionQueueItem();
        item.setTestProcedureId(request.testProcedureId()); item.setTestStepId(request.testStepId()); item.setSuggestionText(result.suggestion()); item.setConfidence(result.confidence()); item.setSource(result.source()); item.setRequestedBy(engineer);
        return AiSuggestionQueueResponse.from(repository.save(item));
    }
    public List<AiSuggestionQueueResponse> pending(String engineer) { return repository.findByRequestedByAndStatusOrderByCreatedAtDesc(engineer, AiSuggestionStatus.PENDING).stream().map(AiSuggestionQueueResponse::from).toList(); }
    public List<AiSuggestionQueueResponse> acceptedForProcedure(Long procedureId, String engineer) {
        return repository.findByRequestedByAndTestProcedureIdAndStatusOrderByDecidedAtDesc(engineer, procedureId, AiSuggestionStatus.ACCEPTED)
                .stream().map(AiSuggestionQueueResponse::from).toList();
    }
    @Transactional
    public AiSuggestionQueueResponse decide(Long id, AiSuggestionStatus decision, String engineer) {
        if (decision == AiSuggestionStatus.PENDING) throw new IllegalArgumentException("A queue item must be accepted or dismissed");
        AiSuggestionQueueItem item = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Suggestion not found: " + id));
        if (!item.getRequestedBy().equals(engineer)) throw new IllegalArgumentException("Only the requesting engineer can decide this suggestion");
        if (item.getStatus() != AiSuggestionStatus.PENDING) throw new IllegalStateException("Suggestion has already been decided");
        item.setStatus(decision); item.setDecidedBy(engineer); item.setDecidedAt(LocalDateTime.now());
        AiSuggestionQueueItem saved = repository.save(item);
        auditService.record(engineer, decision.name() + "_AI_SUGGESTION", "AiSuggestionQueueItem", saved.getId().toString(), null, saved.getSuggestionText(), "Explicit engineer decision; no procedure content is changed automatically.");
        return AiSuggestionQueueResponse.from(saved);
    }
}
