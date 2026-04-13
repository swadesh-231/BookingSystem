package com.bookingsystem.controller;

import com.bookingsystem.dto.*;
import com.bookingsystem.service.HotelService;
import com.bookingsystem.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotel Search", description = "Public endpoints for searching hotels and viewing hotel info")
public class HotelSearchController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;
    private final MeterRegistry meterRegistry;

    @Operation(summary = "Search available hotels", description = "Searches hotels by city, dates, and room count. Returns paginated results with average price.")
    @RateLimiter(name = "searchLimit", fallbackMethod = "searchFallback")
    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceResponse>> searchHotels(@Valid @ModelAttribute HotelSearchRequest hotelSearchRequest) {
        meterRegistry.counter("hotels.search.requests", "city", hotelSearchRequest.getCity()).increment();
        var page = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }

    public ResponseEntity<Page<HotelPriceResponse>> searchFallback(Exception e) {
        throw new com.bookingsystem.exception.APIException("Too many search requests. Please wait a moment.");
    }

    @Operation(summary = "Get hotel info", description = "Returns hotel details including all room types. Public endpoint.")
    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
}
