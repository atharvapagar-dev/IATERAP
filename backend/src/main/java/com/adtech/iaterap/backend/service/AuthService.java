package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.LoginResponse;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Invalid username or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        return refreshTokenService.issue(user);
    }

    /**
     * Rotates a refresh token: the presented token is revoked and a brand
     * new access/refresh token pair is issued in its place. AI-156.
     */
    public LoginResponse refresh(String refreshToken) {
        return refreshTokenService.rotate(refreshToken);
    }

    /**
     * Revokes the session backing a single refresh token. AI-156.
     */
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
