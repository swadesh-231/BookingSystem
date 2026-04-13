package com.bookingsystem.service.impl;

import com.bookingsystem.dto.AdminUserDto;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.dto.PlatformStatsDto;
import com.bookingsystem.entity.*;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.entity.enums.Role;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.HotelRepository;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.security.utils.AuthUtils;
import com.bookingsystem.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private InventoryService inventoryService;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User admin;
    private User regularUser;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L).name("Admin").email("admin@test.com")
                .roles(Set.of(Role.ADMIN)).build();

        regularUser = User.builder()
                .id(2L).name("Regular User").email("user@test.com")
                .roles(Set.of(Role.GUEST)).build();

        hotel = Hotel.builder()
                .id(1L).name("Test Hotel").city("Mumbai")
                .active(false).owner(regularUser).rooms(new ArrayList<>()).build();
    }

    @Nested
    class GetAllUsers {

        @Test
        void shouldReturnPaginatedUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(regularUser), pageable, 1);
            AdminUserDto dto = AdminUserDto.builder().id(2L).email("user@test.com").build();

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(modelMapper.map(regularUser, AdminUserDto.class)).thenReturn(dto);

            Page<AdminUserDto> result = adminService.getAllUsers(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("user@test.com");
        }
    }

    @Nested
    class GetUserById {

        @Test
        void shouldReturnUser() {
            AdminUserDto dto = AdminUserDto.builder().id(2L).build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(modelMapper.map(regularUser, AdminUserDto.class)).thenReturn(dto);

            AdminUserDto result = adminService.getUserById(2L);

            assertThat(result.getId()).isEqualTo(2L);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class UpdateUserRoles {

        @Test
        void shouldUpdateRolesSuccessfully() {
            Set<Role> newRoles = Set.of(Role.GUEST, Role.HOTEL_MANAGER);
            AdminUserDto dto = AdminUserDto.builder().id(2L).roles(newRoles).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(admin);
                when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
                when(modelMapper.map(regularUser, AdminUserDto.class)).thenReturn(dto);

                AdminUserDto result = adminService.updateUserRoles(2L, newRoles);

                assertThat(result.getRoles()).containsExactlyInAnyOrder(Role.GUEST, Role.HOTEL_MANAGER);
                verify(userRepository).save(regularUser);
            }
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteUser() {
            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(admin);
                when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

                adminService.deleteUser(2L);

                verify(userRepository).delete(regularUser);
            }
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetAllHotels {

        @Test
        void shouldReturnPaginatedHotels() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Hotel> hotelPage = new PageImpl<>(List.of(hotel), pageable, 1);
            HotelResponse response = HotelResponse.builder().id(1L).build();

            when(hotelRepository.findAll(pageable)).thenReturn(hotelPage);
            when(modelMapper.map(hotel, HotelResponse.class)).thenReturn(response);

            Page<HotelResponse> result = adminService.getAllHotels(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    class AdminUpdateHotelStatus {

        @Test
        void shouldActivateHotelWithInventoryInit() {
            Room room = Room.builder().id(1L).hotel(hotel).build();
            hotel.setRooms(List.of(room));
            HotelResponse response = HotelResponse.builder().id(1L).active(true).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(admin);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
                when(modelMapper.map(any(Hotel.class), eq(HotelResponse.class))).thenReturn(response);

                HotelResponse result = adminService.updateHotelStatus(1L, true);

                verify(inventoryService).initializeRoomForAYear(room);
                assertThat(result.getActive()).isTrue();
            }
        }
    }

    @Nested
    class GetPlatformStats {

        @Test
        void shouldReturnAggregatedStats() {
            when(userRepository.count()).thenReturn(100L);
            when(hotelRepository.count()).thenReturn(50L);
            when(hotelRepository.countByActive(true)).thenReturn(30L);
            when(bookingRepository.count()).thenReturn(500L);
            when(bookingRepository.sumRevenueByStatus(BookingStatus.CONFIRMED))
                    .thenReturn(new BigDecimal("5000000"));

            PlatformStatsDto result = adminService.getPlatformStats();

            assertThat(result.getTotalUsers()).isEqualTo(100);
            assertThat(result.getTotalHotels()).isEqualTo(50);
            assertThat(result.getActiveHotels()).isEqualTo(30);
            assertThat(result.getTotalBookings()).isEqualTo(500);
            assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("5000000"));
        }

        @Test
        void shouldHandleNullRevenue() {
            when(userRepository.count()).thenReturn(0L);
            when(hotelRepository.count()).thenReturn(0L);
            when(hotelRepository.countByActive(true)).thenReturn(0L);
            when(bookingRepository.count()).thenReturn(0L);
            when(bookingRepository.sumRevenueByStatus(BookingStatus.CONFIRMED)).thenReturn(null);

            PlatformStatsDto result = adminService.getPlatformStats();

            assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class GetAllBookings {

        @Test
        void shouldReturnPaginatedBookings() {
            Pageable pageable = PageRequest.of(0, 20);
            Booking booking = Booking.builder().id(1L).status(BookingStatus.CONFIRMED).build();
            Page<Booking> bookingPage = new PageImpl<>(List.of(booking), pageable, 1);
            BookingResponse response = BookingResponse.builder().id(1L).build();

            when(bookingRepository.findAll(pageable)).thenReturn(bookingPage);
            when(modelMapper.map(booking, BookingResponse.class)).thenReturn(response);

            Page<BookingResponse> result = adminService.getAllBookings(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
