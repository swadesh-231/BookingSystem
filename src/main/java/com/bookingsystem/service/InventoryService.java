package com.bookingsystem.service;


import com.bookingsystem.dto.HotelSearchRequest;
import com.bookingsystem.dto.HotelSearchResponse;
import com.bookingsystem.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {
    void initializeRoomForAYear(Room room);
    void deleteAllInventories(Room room);
    Page<HotelSearchResponse>  searchHotels(HotelSearchRequest hotelSearchRequest);
}
