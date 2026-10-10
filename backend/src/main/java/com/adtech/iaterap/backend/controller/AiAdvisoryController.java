package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.ai.AiAdvisoryClient;
import com.adtech.iaterap.backend.dto.AiSuggestionRequest;
import com.adtech.iaterap.backend.dto.AiSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated backend façade for a structured FastAPI advisory suggestion. */
@RestController
@RequestMapping("/api/ai")
public class AiAdvisoryController {

    private final AiAdvisoryClient client;

    public AiAdvisoryController(AiAdvisoryClient client) {
        this.client = client;
    }

    @PostMapping("/suggestions")
    public AiSuggestionResponse suggest(@Valid @RequestBody AiSuggestionRequest request) {
        return client.suggest(request);
    }
}
