package com.adtech.iaterap.backend.security;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI-276 checklist: expiry, token purpose, and database append-only safeguards. */
class SecurityChecklistTest {

    private static final String SECRET = "iaterap-dev-jwt-signing-key-2026-32-bytes-min";

    @Test
    void expiredJwtIsRejected() {
        JwtService expiredTokenIssuer = new JwtService(SECRET, -1, 7);

        assertFalse(expiredTokenIssuer.validateToken(expiredTokenIssuer.generateAccessToken("engineer")));
    }

    @Test
    void accessAndRefreshTokensHaveDistinctPurposes() {
        JwtService jwt = new JwtService(SECRET, 60, 7);

        assertTrue(jwt.hasTokenType(jwt.generateAccessToken("engineer"), "access"));
        assertTrue(jwt.hasTokenType(jwt.generateRefreshToken("engineer"), "refresh"));
    }

    @Test
    void auditLogMigrationRevokesEveryNonInsertRuntimeOperation() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream(
                "/db/migration/V39__audit_log_append_only_application_grants.sql")) {
            assertNotNull(resource);
            String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("GRANT SELECT, INSERT ON TABLE audit_log TO iaterap_app"));
            assertTrue(sql.contains("REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_log FROM iaterap_app"));
            assertTrue(sql.contains("REVOKE UPDATE, DELETE, TRUNCATE ON TABLE audit_log FROM PUBLIC"));
        }
    }
}
