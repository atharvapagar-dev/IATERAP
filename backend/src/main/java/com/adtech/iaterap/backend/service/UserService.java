package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.UserResponse;
import com.adtech.iaterap.backend.entity.Role;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Minimal user-management service. The only mutating operation currently
 * needed is changing a user's role, which is also the trigger AI-156
 * requires for session revocation: once a user's role changes, every
 * refresh token issued under their previous role is revoked so it can no
 * longer be used to mint new access tokens.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found: " + id
                ));
    }

    /**
     * Updates a user's role and revokes every active refresh-token
     * session they hold, per AI-156's "session revocation on role
     * change" requirement.
     */
    public UserResponse updateRole(Long id, Role newRole) {
        User user = getUserById(id);
        user.setRole(newRole);

        User saved = userRepository.save(user);
        refreshTokenService.revokeAllForUser(saved);

        return UserResponse.fromEntity(saved);
    }
}
