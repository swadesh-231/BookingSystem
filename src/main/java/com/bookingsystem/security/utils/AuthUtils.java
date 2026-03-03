package com.bookingsystem.security.utils;

import com.bookingsystem.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {
    public static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
