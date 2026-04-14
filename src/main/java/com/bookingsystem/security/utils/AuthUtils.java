package com.bookingsystem.security.utils;

import com.bookingsystem.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class AuthUtils {
    public static User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}
