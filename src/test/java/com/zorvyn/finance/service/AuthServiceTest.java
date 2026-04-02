package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.dto.request.LoginRequest;
import com.zorvyn.finance.dto.request.SignupRequest;
import com.zorvyn.finance.dto.response.AuthResponse;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.exception.UnauthorizedException;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock private UserRepository         userRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtTokenProvider       jwtTokenProvider;
    @Mock private AuthenticationManager  authenticationManager;

    @InjectMocks private AuthService authService;

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: should throw BadRequestException when email is already taken")
    void register_duplicateEmail_throwsBadRequestException() {
        SignupRequest request = SignupRequest.builder()
                .email("taken@zorvyn.com")
                .password("Pass@1234")
                .fullName("Duplicate User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("taken@zorvyn.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: should default role to VIEWER when none provided")
    void register_noRoleProvided_defaultsToViewer() {
        SignupRequest request = SignupRequest.builder()
                .email("viewer@zorvyn.com")
                .password("Pass@1234")
                .fullName("Jane Viewer")
                .role(null)          // explicitly no role
                .build();

        User savedUser = User.builder()
                .id(3L)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password("$2a$encoded")
                .role(RoleType.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("jwt.token.here");

        AuthResponse response = authService.register(request);

        assertThat(response.getUser().getRole()).isEqualTo(RoleType.VIEWER);
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("register: should hash password before saving")
    void register_validInput_passwordIsEncoded() {
        SignupRequest request = SignupRequest.builder()
                .email("new@zorvyn.com")
                .password("PlainTextPassword")
                .fullName("New User")
                .role(RoleType.ANALYST)
                .build();

        User savedUser = User.builder()
                .id(4L).email(request.getEmail()).fullName(request.getFullName())
                .password("$2a$hashed").role(RoleType.ANALYST).status(UserStatus.ACTIVE).build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("PlainTextPassword")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("token");

        authService.register(request);

        // Verify the password was encoded — not stored in plain text
        verify(passwordEncoder, times(1)).encode("PlainTextPassword");
        verify(userRepository).save(argThat(u -> "$2a$hashed".equals(u.getPassword())));
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: should throw UnauthorizedException on bad credentials")
    void login_badCredentials_throwsUnauthorizedException() {
        LoginRequest request = LoginRequest.builder()
                .email("user@zorvyn.com")
                .password("wrongpassword")
                .build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("login: should return a valid AuthResponse on correct credentials")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = LoginRequest.builder()
                .email("admin@zorvyn.com")
                .password("correct_password")
                .build();

        User user = User.builder()
                .id(1L).email(request.getEmail()).fullName("Admin")
                .password("$2a$encoded_correct").role(RoleType.ADMIN).status(UserStatus.ACTIVE).build();

        when(authenticationManager.authenticate(any())).thenReturn(null);   // authentication succeeds
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        assertThat(response.getUser().getEmail()).isEqualTo("admin@zorvyn.com");
    }
}
