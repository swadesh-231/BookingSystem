package com.bookingsystem.controller;

import com.bookingsystem.dto.ShowRequestDto;
import com.bookingsystem.dto.ShowResponseDto;
import com.bookingsystem.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/show")
@RequiredArgsConstructor
public class ShowController {
    private final ShowService showService;

    @PostMapping("/add-show")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowResponseDto> createShow(@Valid @RequestBody ShowRequestDto requestDto) {
        ShowResponseDto response = showService.createShow(requestDto);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ShowResponseDto>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }
    @GetMapping("/by-theater")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ShowResponseDto>> getShowsByTheater(@RequestParam String theater) {
        return ResponseEntity.ok(showService.getShowsByTheater(theater));
    }
    @GetMapping("/by-movie")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ShowResponseDto>> getShowsByMovie(@RequestParam String movie) {
        return ResponseEntity.ok(showService.getShowsByMovie(movie));
    }
    @PutMapping("/{showId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowResponseDto> updateShow(@PathVariable Long showId, @Valid @RequestBody ShowRequestDto requestDto) {
        return ResponseEntity.ok(showService.updateShow(showId, requestDto));
    }
    @DeleteMapping("/{showId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShow(@PathVariable Long showId) {
        showService.deleteShow(showId);
        return ResponseEntity.noContent().build();
    }
}
