package com.zorvyn.finance.repository;

import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /** Used by {@code UserDetailsServiceImpl} to reject inactive accounts at login. */
    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    boolean existsByEmail(String email);
}
