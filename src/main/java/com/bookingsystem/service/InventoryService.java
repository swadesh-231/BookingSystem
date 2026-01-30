package com.bookingsystem.service;

import com.bookingsystem.dto.HotelResponse;
import com.bookingsystem.dto.HotelSearchRequest;
import com.bookingsystem.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {
    void initializeRoomForAYear(Room room);
    void deleteAllInventories(Room room);
    Page<HotelResponse>  searchHotels(HotelSearchRequest hotelSearchRequest);
}
