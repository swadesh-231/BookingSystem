package com.bookingsystem.service;

import com.bookingsystem.dto.MovieRequestDto;
import com.bookingsystem.dto.MovieResponseDto;
import com.bookingsystem.entity.enums.Genre;

import java.util.List;

public interface MovieService {
    // Create
    MovieResponseDto addMovie(MovieRequestDto movieRequestDto);

    // Read
    List<MovieResponseDto> getAllMovies();
    MovieResponseDto getMovieById(Long id);
    MovieResponseDto getMovieByExactTitle(String title);
    List<MovieResponseDto> getMoviesByLanguage(String language);
    List<MovieResponseDto> getMoviesByGenre(Genre genre);

    // Update
    MovieResponseDto updateMovie(Long id, MovieRequestDto movieRequestDto);

    // Delete
    void deleteMovie(Long id);
}
