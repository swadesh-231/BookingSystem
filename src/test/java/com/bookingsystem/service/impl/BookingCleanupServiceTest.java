package com.bookingsystem.service.impl;

import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCleanupServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private InventoryRepository inventoryRepository;

    @InjectMocks
    private BookingCleanupService cleanupService;

    private Room room;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanupService, "reservationTtlMinutes", 10);

        Hotel hotel = Hotel.builder().id(1L).name("Test Hotel").build();
        room = Room.builder().id(1L).hotel(hotel).build();
    }

    @Test
    void shouldCancelExpiredBookingsAndReleaseInventory() {
        Booking expired1 = Booking.builder()
                .id(1L).room(room).status(BookingStatus.RESERVED)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .roomsCount(2)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .build();

        Booking expired2 = Booking.builder()
                .id(2L).room(room).status(BookingStatus.GUEST_ADDED)
                .checkInDate(LocalDate.now().plusDays(3))
                .checkOutDate(LocalDate.now().plusDays(5))
                .roomsCount(1)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        when(bookingRepository.findExpiredBookings(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(expired1, expired2));

        cleanupService.cleanupExpiredBookings();

        verify(bookingRepository, times(2)).save(argThat(b ->
                b.getStatus() == BookingStatus.CANCELLED));
        verify(inventoryRepository, times(2)).releaseReservedInventory(
                anyLong(), any(), any(), anyInt());
    }

    @Test
    void shouldDoNothingWhenNoExpiredBookings() {
        when(bookingRepository.findExpiredBookings(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        cleanupService.cleanupExpiredBookings();

        verify(bookingRepository, never()).save(any());
        verify(inventoryRepository, never()).releaseReservedInventory(
                anyLong(), any(), any(), anyInt());
    }

    @Test
    void shouldContinueProcessingWhenOneBookingFails() {
        Booking good = Booking.builder()
                .id(1L).room(room).status(BookingStatus.RESERVED)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .roomsCount(1)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .build();

        Booking bad = Booking.builder()
                .id(2L).room(room).status(BookingStatus.RESERVED)
                .checkInDate(LocalDate.now().plusDays(3))
                .checkOutDate(LocalDate.now().plusDays(5))
                .roomsCount(1)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .build();

        when(bookingRepository.findExpiredBookings(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(bad, good));

        // First booking fails on save
        when(bookingRepository.save(bad)).thenThrow(new RuntimeException("DB error"));
        when(bookingRepository.save(good)).thenReturn(good);

        cleanupService.cleanupExpiredBookings();

        // Second booking should still be processed
        verify(bookingRepository).save(good);
        verify(inventoryRepository).releaseReservedInventory(
                eq(room.getId()), eq(good.getCheckInDate()),
                eq(good.getCheckOutDate().minusDays(1)), eq(1));
    }
}
