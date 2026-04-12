package com.bookingsystem.controller;

import com.bookingsystem.dto.InventoryRequest;
import com.bookingsystem.dto.InventoryResponse;
import com.bookingsystem.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "View and update room inventory (HOTEL_MANAGER role required)")
public class InventoryController {
    private final InventoryService inventoryService;

    @Operation(summary = "Get room inventory", description = "Returns all inventory rows for a room, ordered by date. Must own the hotel.")
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<InventoryResponse>> getAllInventoryByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(inventoryService.getAllInventoriesByRoom(roomId));
    }

    @Operation(summary = "Update room inventory", description = "Bulk update surge factor and/or closed status for a date range. Must own the hotel.")
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<Void> updateInventories(@PathVariable Long roomId,
                                                   @Valid @RequestBody InventoryRequest inventoryRequest) {
        inventoryService.updateInventories(roomId, inventoryRequest);
        return ResponseEntity.noContent().build();
    }
}
