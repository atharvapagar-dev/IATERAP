package com.adtech.iaterap.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityPermissionMatrixTest {

    @Test
    void roleChangesAreAdministratorOnly() throws Exception {
        PreAuthorize authorization = UserController.class
                .getMethod("updateRole", Long.class, com.adtech.iaterap.backend.dto.RoleChangeRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("hasRole('ADMIN')", authorization.value());
    }

    @Test
    void auditEvidenceIsLimitedToEngineeringReviewRoles() {
        PreAuthorize authorization = AuditLogController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("hasAnyRole('ADMIN', 'SYSTEM_ENGINEER', 'RELIABILITY_ENGINEER')", authorization.value());
    }
}
