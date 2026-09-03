package com.adtech.iaterap.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import com.adtech.iaterap.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());

            try {
                if (jwtService.validateToken(token)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    String username = jwtService.extractUsername(token);

                    var authorities = userRepository.findByUsername(username)
                            .map(user -> user.getRole() == null ? Collections.<SimpleGrantedAuthority>emptyList()
                                : List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                            .orElse(Collections.emptyList());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    authorities
                            );

                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(authentication);
                    SecurityContextHolder.setContext(securityContext);
                }
            } catch (Exception ignoredTokenParsingFailure) {
                // Malformed/expired tokens simply leave the request unauthenticated.
                // SecurityConfig requires authentication for all non-public
                // endpoints, so such a request is rejected downstream with a 401
                // by the configured AuthenticationEntryPoint; for the small set
                // of permitAll endpoints the actor falls back to "SYSTEM" for
                // audit purposes.
            }
        }

        filterChain.doFilter(request, response);
    }
}
