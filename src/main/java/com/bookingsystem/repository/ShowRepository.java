package com.bookingsystem.repository;

import com.bookingsystem.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show,Long> {
    List<Show> findByTheater_NameIgnoreCase(String theaterName);
    List<Show> findByMovie_NameIgnoreCase(String movieName);
}
