package com.bookingsystem.service.impl;

import com.bookingsystem.dto.ProfileUpdateRequest;
import com.bookingsystem.dto.UserDto;
import com.bookingsystem.entity.User;
import com.bookingsystem.exception.UserNotFoundException;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("id", id));
    }

    @Override
    public void updateProfile(ProfileUpdateRequest profileUpdateRequest) {
        return;
    }

    @Override
    public UserDto getMyProfile() {
        return null;
    }
}
