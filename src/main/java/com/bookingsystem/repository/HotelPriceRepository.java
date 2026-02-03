package com.bookingsystem.repository;

import com.bookingsystem.dto.HotelPriceDto;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.HotelPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface HotelPriceRepository extends JpaRepository<HotelPrice, Long> {
    Optional<HotelPrice> findByHotelAndDate(Hotel hotel, LocalDate date);

    @Query("""
             SELECT new com.bookingsystem.dto.HotelPriceDto(i.hotel, AVG(i.price))
             FROM HotelPrice i
             WHERE i.hotel.city = :city
                 AND i.date BETWEEN :startDate AND :endDate
                 AND i.hotel.active = true
            GROUP BY i.hotel
            """)
    Page<HotelPriceDto> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable);
}
