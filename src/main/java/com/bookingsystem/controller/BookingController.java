package com.bookingsystem.controller;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.enums.BookingStatus;
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
    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<BookingPaymentInitResponse> initiatePayment(@PathVariable Long bookingId) {
        String sessionUrl = bookingService.initiatePayments(bookingId);
        return ResponseEntity.ok(new BookingPaymentInitResponse(sessionUrl));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<BookingStatus> getBookingStatus(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingStatus(bookingId));
    }
}
