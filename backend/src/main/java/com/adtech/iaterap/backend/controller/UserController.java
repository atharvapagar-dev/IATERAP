package com.adtech.iaterap.backend.controller;

import com.adtech.iaterap.backend.dto.RoleChangeRequest;
import com.adtech.iaterap.backend.dto.UserResponse;
import com.adtech.iaterap.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * Minimal user-management surface. Endpoint-level authorization (e.g.
 * restricting this to ADMIN callers) is intentionally out of scope here:
 * no method-security / role-guard infrastructure exists anywhere else in
 * the project yet (every authenticated endpoint is currently reachable by
 * any authenticated user), so adding one just for this endpoint would be
 * a wider architectural change than AI-156 calls for.
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
    public UserResponse updateRole(
            @PathVariable Long id,
            @RequestBody RoleChangeRequest request
    ) {
        return userService.updateRole(id, request.getRole());
    }
}
