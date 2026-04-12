package com.bookingsystem.repository;

import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel hotel);

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Booking> findByUser(User user);

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT b FROM Booking b WHERE b.status IN :statuses AND b.createdAt < :cutoff")
    List<Booking> findExpiredBookings(@Param("statuses") List<BookingStatus> statuses,
                                      @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Booking b WHERE b.status = :status")
    java.math.BigDecimal sumRevenueByStatus(@Param("status") BookingStatus status);
}
