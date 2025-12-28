package com.bookingsystem.repository;

import com.bookingsystem.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    List<Booking> findByUser_UserId(Long userId);
    List<Booking> findByShow_ShowId(Long showId);
    @Query("""
        select coalesce(sum(b.numberOfSeats), 0)
        from Booking b
        where b.show.showId = :showId
        and b.bookingStatus = 'CONFIRMED'
    """)
    int countConfirmedSeatsByShowId(Long showId);
    @Query("""
        select distinct s
        from Booking b join b.seatNumbers s
        where b.show.showId = :showId
        and b.bookingStatus = 'CONFIRMED'
    """)
    List<String> findConfirmedSeatNumbers(Long showId);

    @Query("""
        select b
        from Booking b
        where b.bookingStatus = 'PENDING'
        and b.bookingTime < :expiryTime
    """)
    List<Booking> findExpiredPendingBookings(LocalDateTime expiryTime);

}
