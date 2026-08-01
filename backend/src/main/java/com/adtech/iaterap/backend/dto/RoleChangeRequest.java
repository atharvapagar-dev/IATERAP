package com.adtech.iaterap.backend.dto;

import com.adtech.iaterap.backend.entity.Role;

public class RoleChangeRequest {

    private Role role;

    public RoleChangeRequest() {
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
