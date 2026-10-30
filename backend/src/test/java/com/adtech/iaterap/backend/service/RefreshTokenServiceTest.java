package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.entity.RefreshToken;
import com.adtech.iaterap.backend.entity.Role;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.RefreshTokenRepository;
import com.adtech.iaterap.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtService);
        lenient().when(jwtService.getRefreshTokenTtl()).thenReturn(java.time.Duration.ofDays(7));
        lenient().when(jwtService.hasTokenType(any(), eq("refresh"))).thenReturn(true);
    }

    private User testUser() {
        User user = new User("tester", "tester@iaterap.com", "hashed-password", Role.VIEWER);
        user.setId(42L);
        return user;
    }

    @Test
    void shouldIssuePersistedRefreshTokenOnLogin() {
        User user = testUser();
        when(jwtService.generateAccessToken("tester")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("tester")).thenReturn("refresh-token");
        when(jwtService.extractTokenId("refresh-token")).thenReturn("jti-1");

        LoginResponse response = refreshTokenService.issue(user);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals("jti-1", captor.getValue().getTokenId());
        assertEquals(user, captor.getValue().getUser());
        assertFalse(captor.getValue().isRevoked());
    }

    @Test
    void shouldRotateAValidUnrevokedSession() {
        User user = testUser();
        RefreshToken existingSession = new RefreshToken(
                "old-jti", user, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(6)
        );

        when(jwtService.validateToken("old-refresh-token")).thenReturn(true);
        when(jwtService.extractTokenId("old-refresh-token")).thenReturn("old-jti");
        when(refreshTokenRepository.findByTokenId("old-jti")).thenReturn(Optional.of(existingSession));
        when(jwtService.generateAccessToken("tester")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken("tester")).thenReturn("new-refresh-token");
        when(jwtService.extractTokenId("new-refresh-token")).thenReturn("new-jti");

        LoginResponse response = refreshTokenService.rotate("old-refresh-token");

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        assertTrue(existingSession.isRevoked());
        assertEquals("new-jti", existingSession.getReplacedByTokenId());

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectRotationOfARevokedSession() {
        User user = testUser();
        RefreshToken revokedSession = new RefreshToken(
                "old-jti", user, LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(5)
        );
        revokedSession.setRevoked(true);

        when(jwtService.validateToken("old-refresh-token")).thenReturn(true);
        when(jwtService.extractTokenId("old-refresh-token")).thenReturn("old-jti");
        when(refreshTokenRepository.findByTokenId("old-jti")).thenReturn(Optional.of(revokedSession));

        assertThrows(BadCredentialsException.class,
                () -> refreshTokenService.rotate("old-refresh-token"));
    }

    @Test
    void shouldRejectRotationOfAnInvalidToken() {
        when(jwtService.validateToken("garbage")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> refreshTokenService.rotate("garbage"));

        verify(refreshTokenRepository, never()).findByTokenId(any());
    }

    @Test
    void shouldRejectAnAccessTokenAtTheRefreshBoundary() {
        when(jwtService.validateToken("access-token")).thenReturn(true);
        when(jwtService.hasTokenType("access-token", "refresh")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> refreshTokenService.rotate("access-token"));
        verify(refreshTokenRepository, never()).findByTokenId(any());
    }

    @Test
    void shouldRevokeSessionOnLogout() {
        User user = testUser();
        RefreshToken session = new RefreshToken(
                "jti-1", user, LocalDateTime.now(), LocalDateTime.now().plusDays(7)
        );

        when(jwtService.validateToken("refresh-token")).thenReturn(true);
        when(jwtService.extractTokenId("refresh-token")).thenReturn("jti-1");
        when(refreshTokenRepository.findByTokenId("jti-1")).thenReturn(Optional.of(session));

        refreshTokenService.revoke("refresh-token");

        assertTrue(session.isRevoked());
        verify(refreshTokenRepository).save(session);
    }

    @Test
    void logoutShouldNoOpForAnInvalidToken() {
        when(jwtService.validateToken("garbage")).thenReturn(false);

        refreshTokenService.revoke("garbage");

        verify(refreshTokenRepository, never()).findByTokenId(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRevokeAllActiveSessionsForUserOnRoleChange() {
        User user = testUser();
        RefreshToken sessionOne = new RefreshToken("jti-1", user, LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        RefreshToken sessionTwo = new RefreshToken("jti-2", user, LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByUserIdAndRevokedFalse(42L))
                .thenReturn(List.of(sessionOne, sessionTwo));

        refreshTokenService.revokeAllForUser(user);

        assertTrue(sessionOne.isRevoked());
        assertTrue(sessionTwo.isRevoked());
        verify(refreshTokenRepository).saveAll(eq(List.of(sessionOne, sessionTwo)));
    }
}
