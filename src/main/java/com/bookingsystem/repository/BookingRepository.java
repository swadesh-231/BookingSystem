package com.bookingsystem.repository;

import com.bookingsystem.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking,Long> {

}
