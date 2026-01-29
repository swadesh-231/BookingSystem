package com.bookingsystem.repository;

import com.bookingsystem.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsByNameAndCity(String name, String city);
}
