package com.adtech.iaterap.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(
            @Value("${jwt.secret:iaterap-dev-jwt-signing-key-2026-32-bytes-min}") String secret,
            @Value("${jwt.access-token-ttl-minutes:60}") long accessTokenTtlMinutes,
            @Value("${jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays
    ) {
        byte[] secretBytes = secret.contains("=")
                ? Decoders.BASE64.decode(secret)
                : secret.getBytes(StandardCharsets.UTF_8);

        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    public String generateAccessToken(String username) {
        return buildToken(username, "access", accessTokenTtl);
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, "refresh", refreshTokenTtl);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private String buildToken(
            String username,
            String tokenType,
            Duration ttl
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim("token_type", tokenType)
                .signWith(signingKey)
                .compact();
    }
}
