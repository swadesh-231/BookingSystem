package com.bookingsystem.service.impl;

import com.bookingsystem.dto.RoomRequest;
import com.bookingsystem.dto.RoomResponse;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Room;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.repository.HotelRepository;
import com.bookingsystem.repository.RoomRepository;
import com.bookingsystem.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;

    @Override
    public RoomResponse createNewRoom(Long hotelId, RoomRequest roomRequest) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        Room room = modelMapper.map(roomRequest, Room.class);
        room.setHotel(hotel);
        roomRepository.save(room);
        return modelMapper.map(room, RoomResponse.class);
    }

    @Override
    public List<RoomResponse> getAllRoomsInHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        //bi directional mapping in hotel entity class
        return hotel.getRooms().stream()
                .map(room -> modelMapper.map(room, RoomResponse.class))
                .toList();
    }

    @Override
    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        return modelMapper.map(room, RoomResponse.class);
    }

    @Override
    public void deleteRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        roomRepository.delete(room);
    }

    @Override
    public RoomResponse updateRoomById(Long hotelId, Long roomId, RoomRequest roomRequest) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        if (!room.getHotel().getId().equals(hotelId)) {
            throw new ResourceNotFoundException("Room", "id", roomId);
        }
        modelMapper.map(roomRequest, room);
        room.setId(roomId);
        room.setHotel(hotel);
        roomRepository.save(room);
        return modelMapper.map(room, RoomResponse.class);
    }
}
