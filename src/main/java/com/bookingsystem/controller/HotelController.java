package com.bookingsystem.controller;

import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.HotelReport;
import com.bookingsystem.dto.HotelRequest;
import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.service.BookingService;
import com.bookingsystem.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/hotel")
@RequiredArgsConstructor
@Tag(name = "Hotel Management", description = "CRUD operations for hotel managers (HOTEL_MANAGER role required)")
public class HotelController {
    private final HotelService hotelService;
    private final BookingService bookingService;

    @Operation(summary = "Create hotel", description = "Creates a new hotel (inactive by default). Only the creator becomes the owner.")
    @PostMapping
    public ResponseEntity<HotelResponse> createNewHotel(@Valid @RequestBody HotelRequest hotelRequest) {
        HotelResponse hotel = hotelService.createNewHotel(hotelRequest);
        return ResponseEntity.ok(hotel);
    }

    @Operation(summary = "List own hotels", description = "Returns all hotels owned by the authenticated manager")
    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        return ResponseEntity.ok(hotelService.findAllHotels());
    }

    @Operation(summary = "Get hotel by ID", description = "Returns hotel details. Must be the owner.")
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> getHotelById(@PathVariable Long hotelId) {
        HotelResponse hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }

    @Operation(summary = "Update hotel", description = "Updates hotel details. Must be the owner.")
    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> updateHotelById(@PathVariable Long hotelId,
            @Valid @RequestBody HotelRequest hotelRequest) {
        HotelResponse hotel = hotelService.updateHotelById(hotelId, hotelRequest);
        return ResponseEntity.ok(hotel);
    }

    @Operation(summary = "Activate / deactivate hotel", description = "Toggles hotel active status. Activation initializes 1-year inventory for all rooms; deactivation deletes all inventory.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<HotelResponse> updateHotelStatus(@PathVariable Long id, @RequestParam Boolean status) {
        return ResponseEntity.ok(hotelService.updateHotelStatus(id, status));
    }

    @Operation(summary = "Delete hotel", description = "Deletes hotel along with all rooms and inventory. Must be the owner.")
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId) {
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List hotel bookings", description = "Returns all bookings for a hotel. Must be the owner.")
    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingResponse>> getBookingsByHotelId(@PathVariable Long hotelId) {
        return ResponseEntity.ok(bookingService.getAllBookingsByHotelId(hotelId));
    }

    @Operation(summary = "Hotel revenue report", description = "Returns booking count, total revenue, and average revenue for confirmed bookings in the date range. Defaults to last month.")
    @GetMapping("/{hotelId}/report")
    public ResponseEntity<HotelReport> getHotelReport(@PathVariable Long hotelId,
                                                      @RequestParam(required = false) LocalDate startDate,
                                                      @RequestParam(required = false) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        return ResponseEntity.ok(bookingService.getHotelReport(hotelId, startDate, endDate));
    }
}
