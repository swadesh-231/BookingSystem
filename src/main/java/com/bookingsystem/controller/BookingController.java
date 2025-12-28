package com.bookingsystem.controller;

import com.bookingsystem.dto.BookingRequestDto;
import com.bookingsystem.dto.BookingResponseDto;
import com.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @RequestBody BookingRequestDto bookingRequestDto
    ) {
        BookingResponseDto response =
                bookingService.createBooking(bookingRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                bookingService.getBookingsByUser(userId)
        );
    }
    @GetMapping("/show/{showId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByShow(
            @PathVariable Long showId
    ) {
        return ResponseEntity.ok(
                bookingService.getBookingsByShow(showId)
        );
    }
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Void> confirmBooking(
            @PathVariable Long bookingId
    ) {
        bookingService.confirmBooking(bookingId);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId
    ) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok().build();
    }
}
