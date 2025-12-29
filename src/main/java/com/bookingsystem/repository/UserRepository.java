package com.bookingsystem.repository;

import com.bookingsystem.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(@NotBlank(message = "Username is required") @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters") @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can contain only letters, numbers, and underscores"
    ) String username);

    boolean existsByEmail(@NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") @Size(max = 100, message = "Email cannot exceed 100 characters") String email);
}
