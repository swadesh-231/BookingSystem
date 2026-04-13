package com.bookingsystem.service.impl;

import com.bookingsystem.dto.RoomRequest;
import com.bookingsystem.dto.RoomResponse;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.UnAuthorisedException;
import com.bookingsystem.repository.HotelRepository;
import com.bookingsystem.repository.RoomRepository;
import com.bookingsystem.service.InventoryService;
import com.bookingsystem.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bookingsystem.security.utils.AuthUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final ModelMapper modelMapper;

    @Override
    public RoomResponse createNewRoom(Long hotelId, RoomRequest roomRequest) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        User user = getCurrentUser();
        if (!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("You are not the owner of this hotel");
        }
        Room room = modelMapper.map(roomRequest, Room.class);
        room.setHotel(hotel);
        roomRepository.save(room);
        if (hotel.getActive()){
            inventoryService.initializeRoomForAYear(room);
        }
        return modelMapper.map(room, RoomResponse.class);
    }

    @Override
    public List<RoomResponse> getAllRoomsInHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        User user = getCurrentUser();
        if (!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("You are not the owner of this hotel");
        }
        return hotel.getRooms().stream()
                .map(room -> modelMapper.map(room, RoomResponse.class))
                .toList();
    }

    @Override
    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        User user = getCurrentUser();
        if (!user.equals(room.getHotel().getOwner())) {
            throw new UnAuthorisedException("You are not the owner of this room's hotel");
        }
        return modelMapper.map(room, RoomResponse.class);
    }

    @Override
    public void deleteRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        User user = getCurrentUser();
        if (!user.equals(room.getHotel().getOwner())) {
            throw new UnAuthorisedException("You are not the owner of this room's hotel");
        }
        inventoryService.deleteAllInventories(room);
        roomRepository.delete(room);
    }

    @Override
    public RoomResponse updateRoom(Long hotelId, Long roomId, RoomRequest roomRequest) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("This user does not own this hotel with id: "+hotelId);
        }
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
