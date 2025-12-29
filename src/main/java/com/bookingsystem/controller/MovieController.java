package com.bookingsystem.controller;

import com.bookingsystem.dto.MovieRequestDto;
import com.bookingsystem.dto.MovieResponseDto;
import com.bookingsystem.entity.enums.Genre;
import com.bookingsystem.exception.InvalidRequestException;
import com.bookingsystem.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @PostMapping("/add-movie")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDto> addMovie(@Valid @RequestBody MovieRequestDto movieRequestDto) {
        MovieResponseDto response = movieService.addMovie(movieRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public ResponseEntity<List<MovieResponseDto>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDto> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDto> updateMovie(@PathVariable Long id, @RequestBody MovieRequestDto movieRequestDto) {
        return ResponseEntity.ok(movieService.updateMovie(id, movieRequestDto));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.ok("Movie deleted successfully");
    }

    @GetMapping("/genre")
    public ResponseEntity<List<MovieResponseDto>> getMoviesByGenre(@RequestParam String genre) {
        return ResponseEntity.ok(movieService.getMoviesByGenre(parse(genre)));
    }

    @GetMapping("/language")
    public ResponseEntity<List<MovieResponseDto>> getMoviesByLanguage(@RequestParam String language) {
        return ResponseEntity.ok(movieService.getMoviesByLanguage(language));
    }

    @GetMapping("/title")
    public ResponseEntity<MovieResponseDto> getMovieByExactTitle(@RequestParam String title) {
        return ResponseEntity.ok(movieService.getMovieByExactTitle(title));
    }
    private Genre parse(String genre) {
        try {
            return Genre.valueOf(genre.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid genre: " + genre);
        }
    }


}
