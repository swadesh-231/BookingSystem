package com.bookingsystem.controller;

import com.bookingsystem.dto.TheaterRequestDto;
import com.bookingsystem.dto.TheaterResponseDto;
import com.bookingsystem.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/theater")
@RequiredArgsConstructor
public class TheaterController {
    private final TheaterService theaterService;

    @PostMapping("/add-theater")
    public ResponseEntity<TheaterResponseDto> createTheater(@Valid @RequestBody TheaterRequestDto requestDto) {
        TheaterResponseDto response = theaterService.createTheater(requestDto);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<TheaterResponseDto>> getTheatersByLocation(@RequestParam String location) {
        return ResponseEntity.ok(theaterService.getTheatersByLocation(location));
    }
    @PutMapping("/{id}")
    public ResponseEntity<TheaterResponseDto> updateTheater(@PathVariable Long id, @Valid @RequestBody TheaterRequestDto requestDto) {
        TheaterResponseDto response = theaterService.updateTheater(id, requestDto);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheater(@PathVariable Long id) {
        theaterService.deleteTheater(id);
        return ResponseEntity.noContent().build();
    }
}
