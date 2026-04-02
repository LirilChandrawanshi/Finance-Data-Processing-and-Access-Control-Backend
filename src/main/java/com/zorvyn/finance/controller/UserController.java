package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.request.UpdateUserRequest;
import com.zorvyn.finance.dto.response.ApiResponse;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only user management endpoints.
 *
 * <p>All methods in this controller are protected with {@code @PreAuthorize("hasRole('ADMIN')")}
 * — any attempt by a VIEWER or ANALYST to call these endpoints will result in 403.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    /** List all registered users. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    /** Fetch a single user by their database ID. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    /**
     * Update a user's profile, role, or status.
     *
     * <p>All fields are optional. Send only the fields that need changing.
     * Example — deactivate a user:
     * <pre>{"status": "INACTIVE"}</pre>
     * Example — promote to ANALYST:
     * <pre>{"role": "ANALYST"}</pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Admin updating user id={}", id);
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request)));
    }
}
