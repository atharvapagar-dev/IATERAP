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
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(
            @Value("${jwt.secret}") String secret,
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
        return buildToken(username, "access", accessTokenTtl, null);
    }

    /**
     * Generates a refresh token carrying a random, unique "jti" claim.
     * AI-156's rotation/revocation tracking (see RefreshTokenService) looks
     * up refresh tokens by this id rather than by the raw token value, so
     * a revoked or rotated-away token can be rejected even though its
     * signature and expiry both still check out.
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, "refresh", refreshTokenTtl, UUID.randomUUID().toString());
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    /**
     * Extracts the "jti" claim from a refresh token. Only meaningful for
     * tokens produced by {@link #generateRefreshToken(String)}; callers
     * must call {@link #validateToken(String)} first to confirm the
     * signature/expiry are valid before trusting this value.
     */
    public String extractTokenId(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
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

    /** Returns whether a signed, non-expired JWT was issued for the supplied purpose. */
    public boolean hasTokenType(String token, String expectedType) {
        try {
            return expectedType.equals(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("token_type", String.class));
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
            Duration ttl,
            String tokenId
    ) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim("token_type", tokenType);

        if (tokenId != null) {
            builder = builder.id(tokenId);
        }

        return builder
                .signWith(signingKey)
                .compact();
    }
}
