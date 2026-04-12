package com.bookingsystem.service.impl;

import com.bookingsystem.dto.ProfileUpdateRequest;
import com.bookingsystem.dto.UserDto;
import com.bookingsystem.entity.User;
import com.bookingsystem.exception.UserNotFoundException;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bookingsystem.security.utils.AuthUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

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
    @Transactional
    public void updateProfile(ProfileUpdateRequest profileUpdateRequest) {
        User user = getCurrentUser();
        if (profileUpdateRequest.getName() != null) {
            user.setName(profileUpdateRequest.getName());
        }
        if (profileUpdateRequest.getDateOfBirth() != null) {
            user.setDateOfBirth(profileUpdateRequest.getDateOfBirth());
        }
        if (profileUpdateRequest.getGender() != null) {
            user.setGender(profileUpdateRequest.getGender());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getMyProfile() {
        User user = getCurrentUser();
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new com.bookingsystem.exception.APIException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
