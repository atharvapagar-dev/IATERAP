package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.Role;
import com.adtech.iaterap.backend.entity.User;

/**
 * Safe, password-free projection of a User, returned by user-management
 * endpoints (e.g. the AI-156 role-change endpoint) instead of the raw
 * entity.
 */
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String email, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
