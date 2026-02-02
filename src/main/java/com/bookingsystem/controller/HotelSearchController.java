package com.bookingsystem.controller;

import com.bookingsystem.dto.HotelInfoDto;
import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.dto.HotelSearchRequest;
import com.bookingsystem.dto.HotelSearchResponse;
import com.bookingsystem.service.HotelService;
import com.bookingsystem.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelSearchController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
    public ResponseEntity<Page<HotelSearchResponse>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {
        var page = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }
    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }

}
