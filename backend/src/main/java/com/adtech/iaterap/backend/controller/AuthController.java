package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.dto.LoginRequest;
import com.adtech.iaterap.backend.dto.RefreshTokenRequest;
import com.adtech.iaterap.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        return authService.login(
                request.getUsername(),
                request.getPassword()
        );
    }

    /**
     * Rotates a refresh token: the presented token is revoked and a new
     * access/refresh token pair is returned. AI-156.
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    /**
     * Revokes the session backing the presented refresh token. AI-156.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
