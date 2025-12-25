package com.bookingsystem.repository;

import com.bookingsystem.entity.Movie;
import com.bookingsystem.entity.enums.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie,Long> {
    Optional<Movie> findByNameIgnoreCase(String name);
    List<Movie> findByLanguageIgnoreCase(String language);
    List<Movie> findByGenresContaining(Genre genre);
}
