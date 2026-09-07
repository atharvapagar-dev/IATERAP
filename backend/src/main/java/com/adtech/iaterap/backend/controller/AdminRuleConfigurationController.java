package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.RuleConfigurationResponse;
import com.adtech.iaterap.backend.dto.RuleEnabledRequest;
import com.adtech.iaterap.backend.dto.RulePriorityRequest;
import com.adtech.iaterap.backend.service.RuleConfigurationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRuleConfigurationController {
    private final RuleConfigurationService service;

    public AdminRuleConfigurationController(RuleConfigurationService service) { this.service = service; }

    @GetMapping
    public List<RuleConfigurationResponse> list() { return service.findAll(); }

    @PatchMapping("/{ruleId}/enabled")
    public RuleConfigurationResponse setEnabled(@PathVariable String ruleId, @Valid @RequestBody RuleEnabledRequest request) {
        return service.setEnabled(ruleId, request.enabled());
    }

    @PatchMapping("/{ruleId}/priority")
    public RuleConfigurationResponse setPriority(@PathVariable String ruleId, @Valid @RequestBody RulePriorityRequest request) {
        return service.setPriority(ruleId, request.priority());
    }
}