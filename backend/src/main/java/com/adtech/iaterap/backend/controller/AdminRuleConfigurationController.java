package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.RuleConfigurationResponse;
import com.adtech.iaterap.backend.dto.ApiResponse;
import com.adtech.iaterap.backend.dto.RuleEnabledRequest;
import com.adtech.iaterap.backend.dto.RulePriorityRequest;
import com.adtech.iaterap.backend.service.RuleConfigurationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRuleConfigurationController {
    private final RuleConfigurationService service;

    public AdminRuleConfigurationController(RuleConfigurationService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<RuleConfigurationResponse>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100.");
        List<RuleConfigurationResponse> eligible = service.findAll().stream()
                .sorted(Comparator.comparing(RuleConfigurationResponse::ruleId))
                .filter(rule -> cursor == null || rule.ruleId().compareTo(cursor) > 0).toList();
        List<RuleConfigurationResponse> page = eligible.stream().limit(limit).toList();
        String nextCursor = eligible.size() > page.size() ? page.getLast().ruleId() : null;
        return ApiResponse.page(page, page.size(), nextCursor);
    }

    @PatchMapping("/{ruleId}/enabled")
    public ApiResponse<RuleConfigurationResponse> setEnabled(@PathVariable String ruleId, @Valid @RequestBody RuleEnabledRequest request) {
        return ApiResponse.of(service.setEnabled(ruleId, request.enabled()));
    }

    @PatchMapping("/{ruleId}/priority")
    public ApiResponse<RuleConfigurationResponse> setPriority(@PathVariable String ruleId, @Valid @RequestBody RulePriorityRequest request) {
        return ApiResponse.of(service.setPriority(ruleId, request.priority()));
    }
}
