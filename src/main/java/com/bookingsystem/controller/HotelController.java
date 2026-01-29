package com.bookingsystem.controller;

import com.bookingsystem.dto.HotelRequest;
import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotel")
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelResponse> createNewHotel(@Valid @RequestBody HotelRequest hotelRequest) {
        HotelResponse hotel = hotelService.createNewHotel(hotelRequest);
        return ResponseEntity.ok(hotel);
    }
    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels() {
        return ResponseEntity.ok(hotelService.findAllHotels());
    }
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> getHotelById(@PathVariable Long hotelId) {
        HotelResponse hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }
    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelResponse> updateHotelById(@Valid @PathVariable Long hotelId,
            @RequestBody HotelRequest hotelRequest) {
        HotelResponse hotel = hotelService.updateHotelById(hotelId, hotelRequest);
        return ResponseEntity.ok(hotel);
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<HotelResponse> updateHotelStatus(@PathVariable Long id, @RequestParam Boolean status) {
        return ResponseEntity.ok(hotelService.updateHotelStatus(id, status));
    }
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId) {
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }
}
