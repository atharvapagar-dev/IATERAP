package com.adtech.iaterap.backend.service;

import com.adtech.iaterap.backend.dto.UserResponse;
import com.adtech.iaterap.backend.entity.Role;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, refreshTokenService);
    }

    private User existingViewer() {
        User user = new User("tester", "tester@iaterap.com", "hashed-password", Role.VIEWER);
        user.setId(7L);
        return user;
    }

    @Test
    void shouldUpdateRoleAndRevokeAllSessions() {
        User user = existingViewer();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateRole(7L, Role.ADMIN);

        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals(7L, response.getId());

        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.updateRole(99L, Role.ADMIN));

        verify(refreshTokenService, never()).revokeAllForUser(any());
    }
}
