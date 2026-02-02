package com.bookingsystem.controller;

import com.bookingsystem.dto.RoomRequest;
import com.bookingsystem.dto.RoomResponse;
import com.bookingsystem.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    @PostMapping
    public ResponseEntity<RoomResponse> createNewRoom(@PathVariable Long hotelId,@Valid @RequestBody RoomRequest roomRequest) {
        RoomResponse room = roomService.createNewRoom(hotelId, roomRequest);
        return ResponseEntity.ok(room);
    }
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRoomsInHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getAllRoomsInHotel(hotelId));
    }
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }
}
