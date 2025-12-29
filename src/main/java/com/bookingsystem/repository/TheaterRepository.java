package com.bookingsystem.repository;

import com.bookingsystem.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TheaterRepository extends JpaRepository<Theater,Long> {
    @Query("""
    SELECT t FROM Theater t
    WHERE LOWER(TRIM(t.location)) = :location
""")
    List<Theater> findByLocationNormalized(@Param("location") String location);

    Optional<Theater> findByNameAndLocationIgnoreCase(String name, String location);
}
