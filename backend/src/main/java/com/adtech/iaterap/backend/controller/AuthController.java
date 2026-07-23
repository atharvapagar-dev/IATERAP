package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.dto.LoginRequest;
import com.adtech.iaterap.backend.service.AuthService;
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
}
