package com.bookingsystem.controller;

import com.bookingsystem.dto.*;
import com.bookingsystem.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/platform")
@RequiredArgsConstructor
@Tag(name = "Platform Admin", description = "Platform-wide administration (ADMIN role required)")
public class AdminController {
    private final AdminService adminService;

    // -- User Management --

    @Operation(summary = "List all users", description = "Returns paginated list of all registered users with their roles")
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, Math.min(size, 100))));
    }

    @Operation(summary = "Get user by ID", description = "Returns a single user's details including roles")
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @Operation(summary = "Update user roles", description = "Replaces the user's roles with the provided set")
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<AdminUserDto> updateUserRoles(@PathVariable Long userId,
                                                        @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(adminService.updateUserRoles(userId, request.getRoles()));
    }

    @Operation(summary = "Delete user", description = "Permanently deletes a user account")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // -- Hotel Oversight --

    @Operation(summary = "List all hotels", description = "Returns paginated list of every hotel on the platform regardless of owner")
    @GetMapping("/hotels")
    public ResponseEntity<Page<HotelResponse>> getAllHotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllHotels(PageRequest.of(page, Math.min(size, 100))));
    }

    @Operation(summary = "Activate / deactivate hotel", description = "Toggles any hotel's active status. Activation initializes inventory; deactivation removes it.")
    @PatchMapping("/hotels/{hotelId}/status")
    public ResponseEntity<HotelResponse> updateHotelStatus(@PathVariable Long hotelId,
                                                           @RequestParam Boolean active) {
        return ResponseEntity.ok(adminService.updateHotelStatus(hotelId, active));
    }

    // -- Booking Oversight --

    @Operation(summary = "List all bookings", description = "Returns paginated list of every booking on the platform")
    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllBookings(PageRequest.of(page, Math.min(size, 100))));
    }

    // -- Dashboard --

    @Operation(summary = "Platform statistics", description = "Returns aggregate counts: users, hotels, bookings, and total confirmed revenue")
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDto> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }
}
