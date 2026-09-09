package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.CoverageScoringConfigurationRequest;
import com.adtech.iaterap.backend.dto.RuleConfigurationResponse;
import com.adtech.iaterap.backend.service.RuleConfigurationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/coverage/scoring-config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCoverageScoringConfigurationController {
    private final RuleConfigurationService service;
    public AdminCoverageScoringConfigurationController(RuleConfigurationService service) { this.service = service; }
    @GetMapping public List<RuleConfigurationResponse> get() { return service.getCoverageScoringConfiguration(); }
    @PutMapping public List<RuleConfigurationResponse> update(@Valid @RequestBody CoverageScoringConfigurationRequest request) { return service.updateCoverageScoringConfiguration(request); }
}
