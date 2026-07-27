package com.adtech.iaterap.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.adtech.iaterap.backend.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.LocalDateTime;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtFilter jwtFilter, ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            // Stateless JWT-based API: no server-side HTTP session is created
            // or relied upon for authentication.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/**",
                        "/api/health",
                        "/api/",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()

                // Every other endpoint requires a valid, authenticated JWT.
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                .anyRequest().authenticated()
            )

            .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint(unauthorizedEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
            )

            // Populates the SecurityContext from a valid Bearer token (if present)
            // so both authorization decisions above and the audit interceptor can
            // correctly identify the acting user.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                writeJsonError(
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "Authentication is required to access this resource."
                );
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJsonError(
                        response,
                        HttpStatus.FORBIDDEN,
                        "You do not have permission to access this resource."
                );
    }

    private void writeJsonError(
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws java.io.IOException {
        ApiError apiError = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                LocalDateTime.now()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }
}
