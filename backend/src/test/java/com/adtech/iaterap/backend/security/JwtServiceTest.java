package com.adtech.iaterap.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "iaterap-dev-jwt-signing-key-2026-32-bytes-min",
                60,
                7
        );
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken("admin");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.validateToken(token));
        assertEquals("admin", jwtService.extractUsername(token));
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String token = jwtService.generateRefreshToken("tester");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.validateToken(token));
        assertEquals("tester", jwtService.extractUsername(token));
    }
    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken("viewer");
        // Replace signature segment with garbage — guaranteed invalid regardless of JVM.
        int lastDot = token.lastIndexOf('.');
        String tamperedToken = token.substring(0, lastDot + 1) + "invalidsignatureXXXXXXXXXXXXXXXX";
        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    void shouldEmbedUniqueTokenIdInEachRefreshToken() {
        String tokenOne = jwtService.generateRefreshToken("tester");
        String tokenTwo = jwtService.generateRefreshToken("tester");

        String tokenIdOne = jwtService.extractTokenId(tokenOne);
        String tokenIdTwo = jwtService.extractTokenId(tokenTwo);

        assertNotNull(tokenIdOne);
        assertNotNull(tokenIdTwo);
        assertNotEquals(tokenIdOne, tokenIdTwo);
    }
}

