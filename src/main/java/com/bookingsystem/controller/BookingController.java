package com.bookingsystem.controller;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking lifecycle: reserve, add guests, pay, cancel, status")
public class BookingController {
    private final BookingService bookingService;
    private final MeterRegistry meterRegistry;

    @Operation(summary = "Initialize booking", description = "Reserves inventory with pessimistic lock. 5-minute TTL before expiry.")
    @RateLimiter(name = "bookingInitLimit", fallbackMethod = "bookingInitFallback")
    @PostMapping("/init")
    public ResponseEntity<BookingResponse> initialiseBooking(
            @RequestBody @jakarta.validation.Valid BookingRequest bookingRequest) {
        BookingResponse response = bookingService.initialiseBooking(bookingRequest);
        meterRegistry.counter("bookings.initiated").increment();
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<BookingResponse> bookingInitFallback(Exception e) {
        throw new com.bookingsystem.exception.APIException("Too many booking requests. Please wait a moment and try again.");
    }

    @Operation(summary = "Add guests to booking", description = "Adds guest details. Booking must be in RESERVED state.")
    @PostMapping("/{bookingId}/addguest")
    public ResponseEntity<BookingResponse> addGuests(@PathVariable Long bookingId,
            @RequestBody @jakarta.validation.Valid List<@jakarta.validation.Valid GuestDto> guestList) {
        return ResponseEntity.ok(bookingService.addGuests(bookingId, guestList));
    }

    @Operation(summary = "Initiate payment", description = "Creates a Stripe Checkout session. Returns the session URL for redirect.")
    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<BookingPaymentInitResponse> initiatePayment(@PathVariable Long bookingId) {
        String sessionUrl = bookingService.initiatePayments(bookingId);
        return ResponseEntity.ok(new BookingPaymentInitResponse(sessionUrl));
    }

    @Operation(summary = "Cancel booking", description = "Cancels a booking. Releases inventory and issues Stripe refund if already paid.")
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        meterRegistry.counter("bookings.cancelled.user").increment();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get booking status", description = "Returns the current status of a booking")
    @GetMapping("/{bookingId}/status")
    public ResponseEntity<BookingStatus> getBookingStatus(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingStatus(bookingId));
    }

    @Operation(summary = "Get my bookings", description = "Returns paginated bookings for the authenticated user, ordered by most recent")
    @GetMapping("/my-bookings")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookingService.getMyBookings(PageRequest.of(page, Math.min(size, 100))));
    }
}
