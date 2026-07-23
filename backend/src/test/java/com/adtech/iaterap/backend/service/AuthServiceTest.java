package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jwtService);
    }

    @Test
    void shouldReturnBothTokensOnLogin() {
        when(jwtService.generateAccessToken("admin"))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken("admin"))
                .thenReturn("refresh-token");

        LoginResponse response = authService.login("admin", "admin123");

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        verify(jwtService).generateAccessToken("admin");
        verify(jwtService).generateRefreshToken("admin");
    }
}
