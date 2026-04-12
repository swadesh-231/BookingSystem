package com.bookingsystem.controller;

import com.bookingsystem.dto.RoomRequest;
import com.bookingsystem.dto.RoomResponse;
import com.bookingsystem.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Management", description = "CRUD operations for rooms within a hotel (HOTEL_MANAGER role required)")
public class RoomController {
    private final RoomService roomService;

    @Operation(summary = "Create room", description = "Creates a new room type. Auto-generates 1-year inventory if the hotel is active.")
    @PostMapping
    public ResponseEntity<RoomResponse> createNewRoom(@PathVariable Long hotelId, @Valid @RequestBody RoomRequest roomRequest) {
        RoomResponse room = roomService.createNewRoom(hotelId, roomRequest);
        return ResponseEntity.ok(room);
    }

    @Operation(summary = "List rooms in hotel")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRoomsInHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getAllRoomsInHotel(hotelId));
    }

    @Operation(summary = "Get room by ID")
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @Operation(summary = "Update room", description = "Updates room details. Must own the hotel.")
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRooms(@PathVariable Long hotelId, @PathVariable Long roomId, @Valid @RequestBody RoomRequest roomRequest) {
        return ResponseEntity.ok(roomService.updateRoom(hotelId, roomId, roomRequest));
    }

    @Operation(summary = "Delete room", description = "Deletes room and all associated inventory")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }
}
