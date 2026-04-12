package com.bookingsystem.service.impl;

import com.bookingsystem.dto.AdminUserDto;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.dto.PlatformStatsDto;
import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.entity.enums.Role;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.HotelRepository;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.service.AdminService;
import com.bookingsystem.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static com.bookingsystem.security.utils.AuthUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> modelMapper.map(user, AdminUserDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return modelMapper.map(user, AdminUserDto.class);
    }

    @Override
    @Transactional
    public AdminUserDto updateUserRoles(Long userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User admin = getCurrentUser();
        log.info("ADMIN_AUDIT: Admin {} updated roles for user {} from {} to {}",
                admin.getEmail(), user.getEmail(), user.getRoles(), roles);
        user.setRoles(roles);
        userRepository.save(user);
        return modelMapper.map(user, AdminUserDto.class);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User admin = getCurrentUser();
        log.info("ADMIN_AUDIT: Admin {} deleted user {}", admin.getEmail(), user.getEmail());
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HotelResponse> getAllHotels(Pageable pageable) {
        return hotelRepository.findAll(pageable)
                .map(hotel -> modelMapper.map(hotel, HotelResponse.class));
    }

    @Override
    @Transactional
    public HotelResponse updateHotelStatus(Long hotelId, Boolean active) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        User admin = getCurrentUser();
        log.info("ADMIN_AUDIT: Admin {} changed hotel '{}' (id={}) active status from {} to {}",
                admin.getEmail(), hotel.getName(), hotelId, hotel.getActive(), active);
        hotel.setActive(active);
        if (Boolean.TRUE.equals(active)) {
            for (Room room : hotel.getRooms()) {
                inventoryService.initializeRoomForAYear(room);
            }
        } else {
            for (Room room : hotel.getRooms()) {
                inventoryService.deleteAllInventories(room);
            }
        }
        Hotel updatedHotel = hotelRepository.save(hotel);
        return modelMapper.map(updatedHotel, HotelResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(booking -> modelMapper.map(booking, BookingResponse.class));
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformStatsDto getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalHotels = hotelRepository.count();
        long activeHotels = hotelRepository.countByActive(true);
        long totalBookings = bookingRepository.count();
        BigDecimal totalRevenue = bookingRepository.sumRevenueByStatus(BookingStatus.CONFIRMED);

        return PlatformStatsDto.builder()
                .totalUsers(totalUsers)
                .totalHotels(totalHotels)
                .activeHotels(activeHotels)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }
}
