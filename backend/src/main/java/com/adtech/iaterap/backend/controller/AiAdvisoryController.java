package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.ai.AiAdvisoryClient;
import com.adtech.iaterap.backend.dto.AiSuggestionRequest;
import com.adtech.iaterap.backend.dto.AiSuggestionResponse;
import com.adtech.iaterap.backend.dto.AiSuggestionQueueResponse;
import com.adtech.iaterap.backend.entity.AiSuggestionStatus;
import com.adtech.iaterap.backend.service.AiSuggestionQueueService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated backend façade for a structured FastAPI advisory suggestion. */
@RestController
@RequestMapping("/api/ai")
public class AiAdvisoryController {

    private final AiAdvisoryClient client;
    private final AiSuggestionQueueService queueService;

    public AiAdvisoryController(AiAdvisoryClient client, AiSuggestionQueueService queueService) {
        this.client = client;
        this.queueService = queueService;
    }

    @PostMapping("/suggestions")
    public AiSuggestionResponse suggest(@Valid @RequestBody AiSuggestionRequest request) {
        return client.suggest(request);
    }

    @PostMapping("/suggestions/queue")
    public AiSuggestionQueueResponse queue(@Valid @RequestBody AiSuggestionRequest request, Principal principal) { return queueService.enqueue(request, principal.getName()); }
    @GetMapping("/suggestions/queue")
    public List<AiSuggestionQueueResponse> queue(Principal principal) { return queueService.pending(principal.getName()); }
    @PostMapping("/suggestions/{id}/accept")
    public AiSuggestionQueueResponse accept(@PathVariable Long id, Principal principal) { return queueService.decide(id, AiSuggestionStatus.ACCEPTED, principal.getName()); }
    @PostMapping("/suggestions/{id}/dismiss")
    public AiSuggestionQueueResponse dismiss(@PathVariable Long id, Principal principal) { return queueService.decide(id, AiSuggestionStatus.DISMISSED, principal.getName()); }
}
