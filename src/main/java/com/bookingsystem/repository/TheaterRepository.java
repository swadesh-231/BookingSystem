package com.bookingsystem.repository;

import com.bookingsystem.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TheaterRepository extends JpaRepository<Theater,Long> {
    List<Theater> findByLocationIgnoreCase(String location);
    Optional<Theater> findByNameAndLocationIgnoreCase(String name, String location);
}
