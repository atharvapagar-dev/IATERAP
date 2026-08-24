package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.VersionComparisonResponse;
import com.adtech.iaterap.backend.service.VersionComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-182 / T7-B1: Version Comparison endpoint.
 *
 * <p>Mounted under {@code /api/procedures} to match the existing
 * {@link com.adtech.iaterap.backend.controller.TestProcedureController}
 * base path (the project's procedure endpoints live under
 * {@code /api/procedures}, not {@code /api/test-procedures}), alongside the
 * existing {@code /history} and {@code /history/{versionNumber}} endpoints
 * this compares.</p>
 */
@RestController
@RequestMapping("/api/procedures")
public class VersionComparisonController {

    private final VersionComparisonService versionComparisonService;

    public VersionComparisonController(VersionComparisonService versionComparisonService) {
        this.versionComparisonService = versionComparisonService;
    }

    @GetMapping("/{procedureId}/compare")
    public VersionComparisonResponse compare(
            @PathVariable Long procedureId,
            @RequestParam Integer fromVersion,
            @RequestParam Integer toVersion
    ) {
        return versionComparisonService.compare(procedureId, fromVersion, toVersion);
    }
}
