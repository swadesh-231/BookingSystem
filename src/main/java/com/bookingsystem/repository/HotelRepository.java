package com.bookingsystem.repository;

import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsByNameAndCity(String name, String city);
    List<Hotel> findByOwner(User owner);
    long countByActive(Boolean active);
}
