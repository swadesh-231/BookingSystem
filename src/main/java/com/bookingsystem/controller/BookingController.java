package com.bookingsystem.controller;

import com.bookingsystem.dto.BookingRequest;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.GuestDto;
import com.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/init")
    public ResponseEntity<BookingResponse> initialiseBooking(@RequestBody BookingRequest bookingRequest) {
        return ResponseEntity.ok(bookingService.initialiseBooking(bookingRequest));
    }
    @PostMapping("/{bookingId}/addguest")
    public ResponseEntity<BookingResponse> addGuests(@PathVariable Long bookingId,@RequestBody List<GuestDto> guestList) {
        return ResponseEntity.ok(bookingService.addGuests(bookingId, guestList));
    }
}
