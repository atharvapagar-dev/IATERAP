package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.RoleChangeRequest;
import com.adtech.iaterap.backend.dto.UserResponse;
import com.adtech.iaterap.backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * User-management is restricted to administrators because role changes
 * revoke sessions and materially alter a user's permissions.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Changes a user's role. Per AI-156, this also revokes every active
     * refresh-token session the user currently holds.
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(
            @PathVariable Long id,
            @RequestBody RoleChangeRequest request
    ) {
        return userService.updateRole(id, request.getRole());
    }
}
