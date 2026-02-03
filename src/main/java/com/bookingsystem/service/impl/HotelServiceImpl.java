package com.bookingsystem.service.impl;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.exception.APIException;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.UnAuthorisedException;
import com.bookingsystem.repository.HotelRepository;

import com.bookingsystem.repository.RoomRepository;
import com.bookingsystem.service.HotelService;
import com.bookingsystem.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Override
    public HotelResponse createNewHotel(HotelRequest hotelRequest) {
        if (hotelRepository.existsByNameAndCity(hotelRequest.getName(), hotelRequest.getCity())) {
            throw new APIException("Hotel already exists!");
        }
        Hotel hotel = modelMapper.map(hotelRequest, Hotel.class);
        hotel.setActive(false);
        User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        hotel.setOwner(user);
        hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelResponse.class);
    }

    @Override
    public HotelResponse getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("User is unauthorized to view hotel");
        }
        return modelMapper.map(hotel, HotelResponse.class);
    }

    @Override
    public List<HotelResponse> findAllHotels() {
        List<Hotel> hotels = hotelRepository.findAll();
        return hotels.stream()
                .map(hotel -> modelMapper.map(hotel, HotelResponse.class))
                .toList();
    }

    @Override
    public HotelResponse updateHotelById(Long id, HotelRequest hotelRequest) {
        Hotel existingHotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorisedException("User is unauthorized to view hotel");
        }
        existingHotel.setName(hotelRequest.getName());
        existingHotel.setCity(hotelRequest.getCity());
        existingHotel.setPhotos(hotelRequest.getPhotos());
        existingHotel.setAmenities(hotelRequest.getAmenities());
        existingHotel.getContact().setEmail(hotelRequest.getContact().getEmail());
        existingHotel.getContact().setPhoneNumber(hotelRequest.getContact().getPhoneNumber());
        existingHotel.getContact().setAddress(hotelRequest.getContact().getAddress());
        existingHotel.getContact().setLocation(hotelRequest.getContact().getLocation());
        Hotel updatedHotel = hotelRepository.save(existingHotel);
        return modelMapper.map(updatedHotel, HotelResponse.class);
    }

    @Override
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("User is unauthorized to view hotel");
        }
        for (Room room : hotel.getRooms()) {
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(id);
    }

    @Override
    public HotelResponse updateHotelStatus(Long id, Boolean status) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("User is unauthorized to view hotel");
        }
        hotel.setActive(status);
        if (Boolean.TRUE.equals(status)) {
            for (Room room : hotel.getRooms()) {
                inventoryService.initializeRoomForAYear(room);
            }
        } else {
            for (Room room : hotel.getRooms()) {
                inventoryService.deleteAllInventories(room);
            }
        }
        Hotel updatedHotel = hotelRepository.save(hotel);
        return modelMapper.map(updatedHotel, HotelResponse.class);
    }

    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        List<RoomResponse> rooms = hotel.getRooms().stream().map((element) ->
                modelMapper.map(element, RoomResponse.class)).toList();
        return new HotelInfoDto(modelMapper.map(hotel, HotelSearchResponse.class), rooms);
    }

}
