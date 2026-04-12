package com.bookingsystem.service.impl;

import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupService {
    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;

    @Value("${app.booking.reservation-ttl-minutes}")
    private int reservationTtlMinutes;

    @Scheduled(fixedDelay = 60000) // every 60s after previous run completes
    @Transactional
    public void cleanupExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(reservationTtlMinutes);
        List<BookingStatus> expirableStatuses = List.of(
                BookingStatus.RESERVED,
                BookingStatus.GUEST_ADDED,
                BookingStatus.PAYMENT_PENDING
        );

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(expirableStatuses, cutoff);

        int cleaned = 0;
        for (Booking booking : expiredBookings) {
            try {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                inventoryRepository.releaseReservedInventory(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate().minusDays(1),
                        booking.getRoomsCount()
                );

                cleaned++;
                log.info("Expired booking {} cancelled and inventory released", booking.getId());
            } catch (Exception e) {
                log.error("Failed to clean up expired booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        if (cleaned > 0) {
            log.info("Cleaned up {}/{} expired bookings", cleaned, expiredBookings.size());
        }
    }
}
