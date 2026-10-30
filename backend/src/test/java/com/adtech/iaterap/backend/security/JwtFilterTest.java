package com.adtech.iaterap.backend.security;

import com.adtech.iaterap.backend.entity.Role;
import com.adtech.iaterap.backend.entity.User;
import com.adtech.iaterap.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validAdminTokenReceivesAdminAuthority() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        User admin = new User("admin", "admin@example.com", "unused", Role.ADMIN);
        when(jwtService.validateToken("token")).thenReturn(true);
        when(jwtService.hasTokenType("token", "access")).thenReturn(true);
        when(jwtService.extractUsername("token")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        new JwtFilter(jwtService, userRepository).doFilter(request, new MockHttpServletResponse(),
                (req, response) -> { });

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void refreshTokenCannotAuthenticateAnApiRequest() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(jwtService.validateToken("refresh")).thenReturn(true);
        when(jwtService.hasTokenType("refresh", "access")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer refresh");
        new JwtFilter(jwtService, userRepository).doFilter(request, new MockHttpServletResponse(),
                (req, response) -> { });

        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
    }
}
