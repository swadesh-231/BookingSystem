package com.bookingsystem.service;

import com.bookingsystem.dto.ProfileUpdateRequest;
import com.bookingsystem.dto.UserDto;
import com.bookingsystem.entity.User;

public interface UserService {
    User getUserById(Long id);
    void updateProfile(ProfileUpdateRequest profileUpdateRequest);
    UserDto getMyProfile();
}
