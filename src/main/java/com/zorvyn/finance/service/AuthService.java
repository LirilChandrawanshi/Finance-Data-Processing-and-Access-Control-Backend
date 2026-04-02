package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.dto.request.LoginRequest;
import com.zorvyn.finance.dto.request.SignupRequest;
import com.zorvyn.finance.dto.response.AuthResponse;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with email '" + request.getEmail() + "' already exists.");
        }

        RoleType assignedRole = request.getRole() != null ? request.getRole() : RoleType.VIEWER;

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(assignedRole)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: email={}, role={}", saved.getEmail(), saved.getRole());

        String token = jwtTokenProvider.generateToken(toUserDetails(saved));
        return AuthResponse.of(token, toUserResponse(saved));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            throw new UnauthorizedException("This account has been deactivated. Please contact an administrator.");
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        String token = jwtTokenProvider.generateToken(toUserDetails(user));
        log.info("User logged in: email={}", user.getEmail());
        return AuthResponse.of(token, toUserResponse(user));
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
