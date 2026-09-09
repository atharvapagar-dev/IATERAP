package com.adtech.iaterap.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.*;

class AdminCoverageScoringConfigurationControllerTest {
    @Test void scoringConfigurationEndpointRequiresAdminAuthorization() {
        PreAuthorize authorization = AdminCoverageScoringConfigurationController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("hasRole('ADMIN')", authorization.value());
    }
}
