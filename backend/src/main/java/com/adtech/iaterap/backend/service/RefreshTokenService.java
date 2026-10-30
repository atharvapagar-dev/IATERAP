package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.entity.RefreshToken;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.RefreshTokenRepository;
import com.adtech.iaterap.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI-156: finishes JWT refresh-token rotation and session revocation on
 * top of the existing (Week 3) stateless JwtService.
 * <p>
 * Every refresh token issued to a user is persisted here, keyed by its
 * JWT "jti" claim. This lets the API:
 * <ul>
 *     <li>rotate a refresh token on every use (the presented token is
 *     revoked and replaced by a brand-new one, closing the reuse
 *     window an attacker would otherwise have on a stolen token);</li>
 *     <li>revoke a single session on logout;</li>
 *     <li>revoke every active session for a user on role change, so a
 *     demoted or deactivated account cannot keep refreshing under its
 *     old privileges.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final String INVALID_REFRESH_TOKEN_MESSAGE =
            "Refresh token is invalid, expired, or has been revoked.";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    /**
     * Issues a brand-new access/refresh token pair for a user (used on
     * login) and persists the refresh token's session record.
     */
    public LoginResponse issue(User user) {
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        persist(refreshToken, user);

        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * Rotates a presented refresh token: validates it, confirms its
     * session hasn't already been revoked or rotated away, then revokes
     * it and issues a brand-new access/refresh token pair for the same
     * user.
     */
    @Transactional
    public LoginResponse rotate(String presentedRefreshToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        if (!jwtService.validateToken(presentedRefreshToken)
                || !jwtService.hasTokenType(presentedRefreshToken, "refresh")) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        String tokenId = jwtService.extractTokenId(presentedRefreshToken);
        RefreshToken session = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE));

        if (session.isRevoked() || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        User user = session.getUser();

        String newAccessToken = jwtService.generateAccessToken(user.getUsername());
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());
        String newTokenId = jwtService.extractTokenId(newRefreshToken);

        session.setRevoked(true);
        session.setRevokedAt(LocalDateTime.now());
        session.setReplacedByTokenId(newTokenId);
        refreshTokenRepository.save(session);

        persist(newRefreshToken, user);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Revokes a single session (used on logout). Silently no-ops for an
     * already-invalid/unknown token so logout is always safe to call.
     */
    public void revoke(String presentedRefreshToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()
                || !jwtService.validateToken(presentedRefreshToken)) {
            return;
        }

        String tokenId = jwtService.extractTokenId(presentedRefreshToken);
        refreshTokenRepository.findByTokenId(tokenId).ifPresent(session -> {
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(session);
        });
    }

    /**
     * Revokes every currently-active session belonging to a user. Used on
     * role change, so a user's existing refresh tokens can no longer be
     * used to mint new access tokens under their previous role.
     */
    public void revokeAllForUser(User user) {
        List<RefreshToken> activeSessions =
                refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());

        LocalDateTime now = LocalDateTime.now();
        for (RefreshToken session : activeSessions) {
            session.setRevoked(true);
            session.setRevokedAt(now);
        }

        refreshTokenRepository.saveAll(activeSessions);
    }

    private void persist(String refreshToken, User user) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plus(jwtService.getRefreshTokenTtl());

        refreshTokenRepository.save(new RefreshToken(tokenId, user, issuedAt, expiresAt));
    }
}
