package com.bookingsystem.controller;

import com.bookingsystem.dto.MovieRequestDto;
import com.bookingsystem.dto.MovieResponseDto;
import com.bookingsystem.entity.enums.Genre;
import com.bookingsystem.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping("/add-movie")
    public ResponseEntity<MovieResponseDto> addMovie(@RequestBody MovieRequestDto movieRequestDto) {
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
    public ResponseEntity<MovieResponseDto> updateMovie(@PathVariable Long id, @RequestBody MovieRequestDto movieRequestDto) {
        return ResponseEntity.ok(movieService.updateMovie(id, movieRequestDto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.ok("Movie deleted successfully");
    }

    @GetMapping("/genre")
    public ResponseEntity<List<MovieResponseDto>> getMoviesByGenre(@RequestParam String genre) {
        Genre genreEnum = Genre.valueOf(genre.toUpperCase());
        return ResponseEntity.ok(movieService.getMoviesByGenre(genreEnum));
    }
    @GetMapping("/language")
    public ResponseEntity<List<MovieResponseDto>> searchMoviesByLanguage(@RequestParam String language) {
        return ResponseEntity.ok(movieService.getMoviesByLanguage(language));
    }
    @GetMapping("/title")
    public ResponseEntity<MovieResponseDto> getMovieByExactTitle(@RequestParam String title) {
        return ResponseEntity.ok(movieService.getMovieByExactTitle(title));
    }
}
