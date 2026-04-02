package com.zorvyn.finance.security;

import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges our domain {@link User} entity with Spring Security's {@link UserDetails} model.
 *
 * <p>Key behaviours:
 * <ul>
 *   <li>Only {@code ACTIVE} users can authenticate. Inactive accounts are loaded with
 *       {@code enabled = false}, causing Spring Security to reject login.</li>
 *   <li>The authority is set as {@code "ROLE_<ROLE_NAME>"} to satisfy Spring Security's
 *       {@code hasRole()} prefix convention used in {@code @PreAuthorize}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication attempt for non-existent email: {}", email);
                    return new UsernameNotFoundException("No account found for email: " + email);
                });

        boolean isActive = user.getStatus() == UserStatus.ACTIVE;

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                isActive,   // enabled
                true,       // accountNonExpired
                true,       // credentialsNonExpired
                true,       // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
