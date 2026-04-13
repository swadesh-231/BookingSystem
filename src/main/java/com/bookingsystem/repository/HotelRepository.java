package com.bookingsystem.repository;

import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsByNameAndCity(String name, String city);
    List<Hotel> findByOwner(User owner);
    Page<Hotel> findByOwner(User owner, Pageable pageable);
    long countByActive(Boolean active);

    @org.springframework.data.jpa.repository.Query("SELECT h FROM Hotel h LEFT JOIN FETCH h.rooms WHERE h.id = :id")
    java.util.Optional<Hotel> findByIdWithRooms(@org.springframework.data.repository.query.Param("id") Long id);
}
