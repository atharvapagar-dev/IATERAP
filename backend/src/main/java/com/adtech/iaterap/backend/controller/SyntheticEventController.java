package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.SyntheticEventGenerationResponse;
import com.adtech.iaterap.backend.service.SyntheticEventGenerationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Explicitly synthetic fleet-evidence generation; never accepts live telemetry. */
@RestController
@RequestMapping("/api/reliability/synthetic-events")
@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')")
public class SyntheticEventController {

    private final SyntheticEventGenerationService service;

    public SyntheticEventController(SyntheticEventGenerationService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public SyntheticEventGenerationResponse generate(@RequestParam Long aircraftId) {
        return service.generate(aircraftId);
    }
}
