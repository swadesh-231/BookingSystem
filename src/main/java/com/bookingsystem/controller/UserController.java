package com.bookingsystem.controller;

import com.bookingsystem.dto.ApiResponse;
import com.bookingsystem.dto.ChangePasswordRequest;
import com.bookingsystem.dto.ProfileUpdateRequest;
import com.bookingsystem.dto.UserDto;
import com.bookingsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "View and update user profile, change password")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile details")
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @Operation(summary = "Update profile", description = "Updates name, gender, and/or date of birth. Null fields are ignored.")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest profileUpdateRequest) {
        userService.updateProfile(profileUpdateRequest);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Profile updated successfully")
                .status(true)
                .build());
    }

    @Operation(summary = "Change password", description = "Requires current password for verification. New password must meet strength requirements.")
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Password changed successfully")
                .status(true)
                .build());
    }
}
