package com.bookingsystem.service;

import com.bookingsystem.dto.RoomRequest;
import com.bookingsystem.dto.RoomResponse;

import java.util.List;

public interface RoomService {
    RoomResponse createNewRoom(Long hotelId, RoomRequest roomRequest);
    List<RoomResponse> getAllRoomsInHotel(Long hotelId);
    RoomResponse getRoomById(Long roomId);
    void deleteRoomById(Long roomId);
    RoomResponse updateRoomById(Long hotelId, Long roomId, RoomRequest roomRequest);
}
