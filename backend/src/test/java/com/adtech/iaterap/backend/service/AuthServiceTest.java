package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.entity.Role;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.UserRepository;
import com.adtech.iaterap.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private User existingAdmin() {
        return new User("admin", "admin@iaterap.com", "hashed-password", Role.ADMIN);
    }

    @Test
    void shouldReturnBothTokensOnValidLogin() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(existingAdmin()));
        when(passwordEncoder.matches("admin123", "hashed-password"))
                .thenReturn(true);
        when(jwtService.generateAccessToken("admin")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("admin")).thenReturn("refresh-token");

        LoginResponse response = authService.login("admin", "admin123");

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        verify(jwtService).generateAccessToken("admin");
        verify(jwtService).generateRefreshToken("admin");
    }

    @Test
    void shouldRejectUnknownUsername() {
        when(userRepository.findByUsername("ghost"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login("ghost", "anything")
        );

        verify(jwtService, never()).generateAccessToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectWrongPassword() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(existingAdmin()));
        when(passwordEncoder.matches("wrong-password", "hashed-password"))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login("admin", "wrong-password")
        );

        verify(jwtService, never()).generateAccessToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login("  ", "admin123")
        );
    }

    @Test
    void shouldRejectBlankPassword() {
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login("admin", "")
        );
    }
}
