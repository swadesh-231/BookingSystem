package com.bookingsystem.repository;

import com.bookingsystem.entity.Movie;
import com.bookingsystem.entity.enums.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie,Long> {
    @Query("SELECT DISTINCT m FROM Movie m JOIN m.genres g WHERE g = :genre")
    List<Movie> findByGenre(@Param("genre") Genre genre);

    List<Movie> findByLanguageIgnoreCase(String language);
    Optional<Movie> findByNameIgnoreCase(String name);
}
