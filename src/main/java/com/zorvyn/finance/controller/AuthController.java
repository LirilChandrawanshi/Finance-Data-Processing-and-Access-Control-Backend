package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.request.LoginRequest;
import com.zorvyn.finance.dto.request.SignupRequest;
import com.zorvyn.finance.dto.response.ApiResponse;
import com.zorvyn.finance.dto.response.AuthResponse;
import com.zorvyn.finance.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user account.
     *
     * <p>Returns a JWT token immediately on success so the client can start
     * making authenticated requests without a separate login round-trip.
     *
     * @param request signup details including email, password, full name, and optional role
     * @return 201 Created with JWT token and user profile
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup attempt for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Authenticate with email and password.
     *
     * @param request login credentials
     * @return 200 OK with JWT token and user profile
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
