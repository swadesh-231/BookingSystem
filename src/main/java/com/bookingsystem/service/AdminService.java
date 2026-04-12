package com.bookingsystem.service;

import com.bookingsystem.dto.AdminUserDto;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.dto.PlatformStatsDto;
import com.bookingsystem.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface AdminService {
    Page<AdminUserDto> getAllUsers(Pageable pageable);
    AdminUserDto getUserById(Long userId);
    AdminUserDto updateUserRoles(Long userId, Set<Role> roles);
    void deleteUser(Long userId);

    Page<HotelResponse> getAllHotels(Pageable pageable);
    HotelResponse updateHotelStatus(Long hotelId, Boolean active);

    Page<BookingResponse> getAllBookings(Pageable pageable);

    PlatformStatsDto getPlatformStats();
}
